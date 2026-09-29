package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.entity.AnimalEntity;
import com.vetplanet.modules.cliente.exception.AnimalComHistoricoException;
import com.vetplanet.modules.cliente.exception.AnimalNaoEncontradoException;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exclui de verdade o cadastro de um animal.
 *
 * <p><b>É a única exclusão física do sistema, e é deliberada.</b> O
 * {@code CLAUDE.md} proíbe apagar consulta, prontuário e usuário — animal não
 * está nessa lista, e o motivo da regra não se aplica a um cadastro criado por
 * engano: não há histórico a preservar. Inventar um status "removido" para
 * esse caso deixaria lixo permanente na ficha do tutor, que é exatamente o que
 * a veterinária não quer ver.
 *
 * <p><b>O que protege o histórico é a chave estrangeira</b>, não uma checagem
 * que alguém precisa lembrar de escrever. Quando {@code agendamento.consulta}
 * e {@code prontuario} passarem a referenciar {@code cliente.animal}, o banco
 * recusa o delete sozinho — e continua recusando daqui a cinco anos, sem
 * depender de ninguém manter esta classe atualizada.
 *
 * <p>O {@code flush()} é o que faz a violação estourar <b>dentro</b> do try:
 * sem ele, a constraint só dispara no commit, fora do alcance do catch, e o
 * usuário receberia um 500 genérico em vez da explicação.
 */
@Service
public class ExcluirAnimalService {

    private static final Logger log = LoggerFactory.getLogger(ExcluirAnimalService.class);

    private final AnimalRepository animalRepository;

    public ExcluirAnimalService(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    @Transactional
    public void excluirAnimal(UUID idAnimal) {
        AnimalEntity animal =
                animalRepository
                        .findById(idAnimal)
                        .orElseThrow(() -> new AnimalNaoEncontradoException(idAnimal));

        String nome = animal.getNome();

        try {
            animalRepository.delete(animal);
            animalRepository.flush();
        } catch (DataIntegrityViolationException erro) {
            log.info("Exclusão barrada: o animal {} tem histórico", idAnimal);
            throw new AnimalComHistoricoException(nome);
        }

        log.info("Animal {} excluído do cadastro", idAnimal);
    }
}
