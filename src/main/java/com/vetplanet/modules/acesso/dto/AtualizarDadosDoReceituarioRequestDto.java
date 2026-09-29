package com.vetplanet.modules.acesso.dto;

import jakarta.validation.constraints.Size;

/**
 * Os dados de contato que aparecem no rodapé do receituário.
 *
 * <p><b>Todos opcionais.</b> São dados de vitrine, não de identificação: o que
 * a norma exige do documento — nome completo e CRMV — vive em outro lugar e
 * não passa por aqui. Um veterinário sem Instagram continua emitindo receita.
 *
 * <p>Nome, e-mail e perfil <b>não entram</b>: mudar o próprio perfil de acesso
 * seria escalar privilégio, e o nome no prontuário é o que valia na época do
 * atendimento — quem corrige cadastro é o administrador, por outra rota.
 */
public record AtualizarDadosDoReceituarioRequestDto(
        @Size(max = 40, message = "Telefone muito longo.") String telefoneContato,
        @Size(max = 60, message = "Instagram muito longo.") String instagram,
        @Size(max = 120, message = "Site muito longo.") String site,
        @Size(max = 80, message = "Cidade muito longa.") String cidadeAtuacao) {}
