package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.dto.CriarAnimalRequestDto;
import com.vetplanet.modules.cliente.dto.AnimalResponseDto;
import com.vetplanet.modules.cliente.dto.CriarTutorRequestDto;
import com.vetplanet.modules.cliente.dto.TutorResponseDto;
import com.vetplanet.modules.cliente.entity.AnimalEntity;
import com.vetplanet.modules.cliente.entity.TutorEntity;
import com.vetplanet.modules.cliente.exception.TutorSemAnimalException;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import com.vetplanet.modules.cliente.repository.TutorRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastra um tutor com os animais dele, numa transação só.
 *
 * <p>Tutor e animais juntos porque é assim que a informação chega — e porque
 * separar deixaria tutor órfão quando a segunda chamada falhasse.
 */
@Service
public class CriarTutorService {

    private static final Logger log = LoggerFactory.getLogger(CriarTutorService.class);

    private final TutorRepository tutorRepository;
    private final AnimalRepository animalRepository;

    public CriarTutorService(TutorRepository tutorRepository, AnimalRepository animalRepository) {
        this.tutorRepository = tutorRepository;
        this.animalRepository = animalRepository;
    }

    @Transactional
    public TutorResponseDto criarTutor(CriarTutorRequestDto request) {
        // O @NotEmpty do DTO já barra, mas a regra é de domínio e não pode
        // depender de quem chamou ter passado pela validação da borda HTTP.
        if (request.animais() == null || request.animais().isEmpty()) {
            throw new TutorSemAnimalException();
        }

        TutorEntity tutor =
                TutorEntity.criar(
                        request.nomeCompleto(),
                        request.telefoneWhatsapp(),
                        request.email(),
                        request.endereco() == null ? null : request.endereco().paraEntidade(),
                        request.observacoes());

        tutorRepository.save(tutor);

        List<AnimalEntity> animais =
                request.animais().stream().map(dados -> montarAnimal(tutor, dados)).toList();
        animalRepository.saveAll(animais);

        // Sem nome nem telefone no log: são dados pessoais (ver LGPD no CLAUDE.md).
        log.info("Tutor {} cadastrado com {} animal(is)", tutor.getId(), animais.size());

        // Tutor recém-criado: por definição nenhum animal dele tem histórico
        // ainda, então não há o que perguntar ao domínio vizinho.
        return TutorResponseDto.completo(
                tutor, animais.stream().map(animal -> AnimalResponseDto.de(animal, true)).toList());
    }

    private static AnimalEntity montarAnimal(TutorEntity tutor, CriarAnimalRequestDto dados) {
        return AnimalEntity.criar(
                tutor,
                dados.nome(),
                dados.especie(),
                dados.raca(),
                dados.sexo(),
                dados.dataNascimento(),
                dados.castrado(),
                dados.observacoes());
    }
}
