package com.kellyacademy.user.repository;

import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import com.kellyacademy.user.specification.UsuarioSpecifications;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UsuarioFiltrosJpaTest {
    @Autowired private UsuarioRepository usuarios;
    @Autowired private EntityManager em;

    @BeforeEach
    void crearDatos() {
        Rol estudiante = rol("ESTUDIANTE");
        Rol docente = rol("DOCENTE");
        usuario("Ana", "Perez", "ana@example.test", EstadoUsuario.ACTIVO, estudiante);
        usuario("Ana", "Diaz", "otra@example.test", EstadoUsuario.INACTIVO, estudiante);
        usuario("Ana", "Torres", "profesor@example.test", EstadoUsuario.ACTIVO, docente);
        usuario("Luis", "Perez", "luis@example.test", EstadoUsuario.ACTIVO, estudiante);
        em.flush();
        em.clear();
    }

    @Test
    void combinaTextoEstadoYRolSinPerderPaginacion() {
        var pagina = usuarios.findAll(UsuarioSpecifications.filtros("ANA", EstadoUsuario.ACTIVO, "estudiante"), PageRequest.of(0, 1));
        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent()).extracting(Usuario::getCorreoElectronico).containsExactly("ana@example.test");
    }

    @Test
    void buscaNombreCompletoYTrataComodinesComoLiterales() {
        assertThat(usuarios.findAll(UsuarioSpecifications.filtros("ana perez", null, null), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(usuarios.findAll(UsuarioSpecifications.filtros("%", null, null), PageRequest.of(0, 10)).getTotalElements()).isZero();
    }

    @Test
    void sinFiltrosDevuelveTodasLasCuentas() {
        assertThat(usuarios.findAll(UsuarioSpecifications.filtros(null, null, null), PageRequest.of(0, 2)).getTotalElements()).isEqualTo(4);
    }

    private Rol rol(String nombre) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        em.persist(rol);
        return rol;
    }

    private void usuario(String nombre, String apellido, String correo, EstadoUsuario estado, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setCorreoElectronico(correo);
        usuario.setContrasena("hash-test");
        usuario.setEstado(estado);
        usuario.setRoles(Set.of(rol));
        em.persist(usuario);
    }
}
