package com.vetplanet.modules.prontuario.service;

import com.vetplanet.modules.acesso.entity.UsuarioEntity;
import com.vetplanet.modules.acesso.service.BuscarUsuarioAtualService;
import com.vetplanet.modules.prontuario.dto.AtendimentoResponseDto;
import com.vetplanet.modules.prontuario.dto.RegistrarAtendimentoRequestDto;
import com.vetplanet.modules.prontuario.entity.AlteracaoAtendimentoEntity;
import com.vetplanet.modules.prontuario.entity.AtendimentoEntity;
import com.vetplanet.modules.prontuario.exception.AtendimentoNaoEncontradoException;
import com.vetplanet.modules.prontuario.exception.SemPermissaoParaAssinarException;
import com.vetplanet.modules.prontuario.repository.AlteracaoAtendimentoRepository;
import com.vetplanet.modules.prontuario.repository.AtendimentoRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Salva o conteúdo clínico — e anota o que mudou.
 *
 * <p><b>Não recusa alterar atendimento concluído.</b> A versão anterior
 * recusava, e a regra caiu por um argumento melhor: a veterinária lembra do
 * nódulo na pata depois de sair da casa, ou a tutora conta algo no portão.
 * Impedir o registro de crescer não protege o prontuário — empobrece, que é o
 * contrário do que a trava pretendia. E é o modelo do próprio CFMV, que pede
 * "evolução diária" (Art. 9º, VIII): registro que cresce.
 *
 * <p><b>O que ficou no lugar da trava é o rastro.</b> Cada campo alterado vira
 * uma linha em {@code alteracao_atendimento}, com quem, quando, o valor antigo
 * e o novo. Um prontuário que se reescreve em silêncio não prova nada; um que
 * registra a mudança prova, e ainda deixa acrescentar.
 *
 * <p>O rastro só começa depois da primeira conclusão. Antes disso o
 * atendimento é rascunho sendo escrito, e gravar cada tecla do primeiro
 * preenchimento encheria a tabela de ruído — a tela salva a cada campo que
 * perde o foco.
 */
@Service
public class RegistrarAtendimentoService {

    private final AtendimentoRepository atendimentoRepository;
    private final AlteracaoAtendimentoRepository alteracaoRepository;
    private final BuscarUsuarioAtualService buscarUsuarioAtualService;

    public RegistrarAtendimentoService(
            AtendimentoRepository atendimentoRepository,
            AlteracaoAtendimentoRepository alteracaoRepository,
            BuscarUsuarioAtualService buscarUsuarioAtualService) {
        this.atendimentoRepository = atendimentoRepository;
        this.alteracaoRepository = alteracaoRepository;
        this.buscarUsuarioAtualService = buscarUsuarioAtualService;
    }

    @Transactional
    public AtendimentoResponseDto registrar(
            UUID idAtendimento, RegistrarAtendimentoRequestDto request, String emailDoUsuario) {
        AtendimentoEntity atendimento =
                atendimentoRepository
                        .findById(idAtendimento)
                        .orElseThrow(() -> new AtendimentoNaoEncontradoException(idAtendimento));

        // Continua valendo quem pode escrever: só veterinário com CRMV, como
        // manda o Art. 9º, II e VIII. O que caiu foi a trava por tempo, não a
        // trava por quem.
        UsuarioEntity autor = buscarUsuarioAtualService.buscarEntidade(emailDoUsuario);
        if (!autor.podeAssinarProntuario()) throw new SemPermissaoParaAssinarException();

        Map<String, String> antes = atendimento.estaConcluido() ? retrato(atendimento) : Map.of();

        atendimento.registrar(
                request.localAtendimento(),
                request.anamnese(),
                request.exameClinico(),
                request.diagnosticoPresuntivo(),
                request.diagnosticoConclusivo(),
                request.recomendacoes(),
                request.prescricao(),
                request.pesoKg(),
                request.temperaturaC(),
                request.frequenciaCardiaca(),
                request.frequenciaRespiratoria());

        if (!antes.isEmpty()) {
            anotarMudancas(atendimento, autor, antes, retrato(atendimento));
        }

        return AtendimentoResponseDto.de(atendimento);
    }

    /**
     * O conteúdo do atendimento como pares campo/valor.
     *
     * <p>{@code LinkedHashMap} para o rastro sair na ordem em que os campos
     * aparecem na tela, e não na ordem aleatória de um hash.
     */
    private static Map<String, String> retrato(AtendimentoEntity atendimento) {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("localAtendimento", atendimento.getLocalAtendimento());
        valores.put("anamnese", atendimento.getAnamnese());
        valores.put("exameClinico", atendimento.getExameClinico());
        valores.put("diagnosticoPresuntivo", atendimento.getDiagnosticoPresuntivo());
        valores.put("diagnosticoConclusivo", atendimento.getDiagnosticoConclusivo());
        valores.put("recomendacoes", atendimento.getRecomendacoes());
        valores.put("prescricao", atendimento.getPrescricao());
        valores.put("pesoKg", texto(atendimento.getPesoKg()));
        valores.put("temperaturaC", texto(atendimento.getTemperaturaC()));
        valores.put("frequenciaCardiaca", texto(atendimento.getFrequenciaCardiaca()));
        valores.put("frequenciaRespiratoria", texto(atendimento.getFrequenciaRespiratoria()));
        return valores;
    }

    /**
     * O valor como texto, para comparar e guardar.
     *
     * <p><b>{@code BigDecimal} passa por {@code stripTrailingZeros}</b>, e não
     * é preciosismo: a coluna é {@code numeric(6,3)}, então o banco devolve
     * {@code 4.500} para o que a tela mandou como {@code 4.5}. Sem normalizar,
     * cada salvamento acusaria uma alteração de peso que não houve — e o
     * rastro, que existe para dizer o que mudou, viraria ruído dizendo que
     * tudo muda sempre.
     */
    private static String texto(Object valor) {
        if (valor == null) return null;
        if (valor instanceof BigDecimal numero) {
            return numero.stripTrailingZeros().toPlainString();
        }
        return valor.toString();
    }

    private void anotarMudancas(
            AtendimentoEntity atendimento,
            UsuarioEntity autor,
            Map<String, String> antes,
            Map<String, String> depois) {
        List<AlteracaoAtendimentoEntity> alteracoes = new ArrayList<>();

        for (Map.Entry<String, String> campo : antes.entrySet()) {
            String anterior = campo.getValue();
            String novo = depois.get(campo.getKey());

            // Salvar sem mexer é o caso comum — a tela grava a cada campo que
            // perde o foco. Sem esta checagem, o rastro viraria ruído.
            if (Objects.equals(anterior, novo)) continue;

            alteracoes.add(
                    AlteracaoAtendimentoEntity.registrar(
                            atendimento.getId(),
                            autor.getId(),
                            autor.getNomeCompleto(),
                            autor.getCrmv(),
                            campo.getKey(),
                            anterior,
                            novo));
        }

        if (!alteracoes.isEmpty()) alteracaoRepository.saveAll(alteracoes);
    }
}
