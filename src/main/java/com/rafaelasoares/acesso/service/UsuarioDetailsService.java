package com.rafaelasoares.acesso.service;

import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponte entre {@code acesso.usuario} e o Spring Security.
 *
 * <p>O login é o e-mail. Nomes em inglês aqui ({@code loadUserByUsername})
 * porque é contrato de framework — quem chama é o Spring, não nosso código.
 *
 * <p><b>Hoje não está no caminho da autenticação.</b> Quem valida a senha é o
 * {@code CriarSessaoService}, e quem autentica cada request é o
 * {@code TokenAutenticacaoFilter}. Este bean permanece por dois motivos:
 *
 * <ul>
 *   <li>Sendo um {@code UserDetailsService}, impede o Spring Boot de criar um
 *       usuário em memória com senha aleatória no boot — que polui o log e
 *       parece defeito de configuração.
 *   <li>É o ponto de extensão natural caso venhamos a usar o
 *       {@code AuthenticationManager} do Spring (por exemplo, num fluxo de
 *       redefinição de senha pelo administrador).
 * </ul>
 *
 * <p>Se nenhum dos dois se confirmar, remova — código sem uso é dívida.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario =
                usuarioRepository
                        .findByEmailIgnoreCase(email)
                        // Mensagem genérica de propósito: dizer "e-mail não
                        // existe" permitiria descobrir quem tem conta.
                        .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .roles(usuario.getPerfilAcesso().name())
                .disabled(!usuario.isAtivo())
                .build();
    }
}
