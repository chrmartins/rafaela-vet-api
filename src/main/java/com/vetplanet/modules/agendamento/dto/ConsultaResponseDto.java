package com.vetplanet.modules.agendamento.dto;

import com.vetplanet.modules.agendamento.entity.ConsultaEntity;
import com.vetplanet.modules.agendamento.entity.StatusConsulta;
import com.vetplanet.modules.cliente.dto.ResumoAnimalDto;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Consulta como sai da API, já com os nomes que a Agenda precisa escrever.
 *
 * <p>Nome de animal e de tutor vêm de `cliente`, resolvidos em lote pelo
 * service — a entidade guarda só os ids, porque `agendamento` não mapeia as
 * tabelas do vizinho.
 *
 * <p>Os nomes podem vir nulos se o animal tiver sido excluído entre a leitura
 * e a resolução. Na prática não acontece (a FK impede excluir animal com
 * consulta), mas o DTO não depende disso para não quebrar.
 */
public record ConsultaResponseDto(
        UUID idConsulta,
        UUID idAnimal,
        String nomeAnimal,
        UUID idTutor,
        String nomeTutor,
        OffsetDateTime dataHora,
        int duracaoMinutos,
        String motivo,
        StatusConsulta status,
        boolean urgente,
        String enderecoAtendimento,
        String observacoes,
        /**
         * A consulta já tem atendimento — rascunho ou concluído?
         *
         * <p>Serve para o cartão da Agenda escolher entre "Iniciar
         * atendimento" e "Continuar atendimento". Combinado com o status,
         * cobre os três estados: {@code CONCLUIDA} quer dizer registro
         * fechado, porque desde que o prontuário existe essa é a única forma
         * de uma consulta concluir.
         *
         * <p><b>Quem monta o DTO precisa informar</b> — não há valor padrão,
         * pelo mesmo motivo do {@code podeExcluir} do animal: um default
         * silencioso voltaria a mentir no dia em que alguém esquecesse de
         * perguntar.
         */
        boolean temRegistroClinico,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {

    public static ConsultaResponseDto de(
            ConsultaEntity consulta, ResumoAnimalDto resumo, boolean temRegistroClinico) {
        return new ConsultaResponseDto(
                consulta.getId(),
                consulta.getIdAnimal(),
                resumo == null ? null : resumo.nomeAnimal(),
                consulta.getIdTutor(),
                resumo == null ? null : resumo.nomeTutor(),
                consulta.getDataHora(),
                consulta.getDuracaoMinutos(),
                consulta.getMotivo(),
                consulta.getStatus(),
                consulta.isUrgente(),
                consulta.getEnderecoAtendimento(),
                consulta.getObservacoes(),
                temRegistroClinico,
                consulta.getCriadoEm(),
                consulta.getAtualizadoEm());
    }
}
