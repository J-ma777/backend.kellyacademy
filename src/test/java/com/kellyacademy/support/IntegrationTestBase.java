package com.kellyacademy.support;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.repository.ClaseRepository;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.course.repository.MaterialRepository;
import com.kellyacademy.course.repository.SemanaRepository;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.course.repository.UnidadRepository;
import com.kellyacademy.enrollment.repository.EntregaRepository;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.security.auth.dto.AuthRequest;
import com.kellyacademy.security.auth.dto.AuthResponse;
import com.kellyacademy.user.entity.Permiso;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import com.kellyacademy.user.enums.PermisoSistema;
import com.kellyacademy.user.repository.PermisoRepository;
import com.kellyacademy.user.repository.RolRepository;
import com.kellyacademy.user.repository.UsuarioRepository;
import com.kellyacademy.attendance.repository.AsistenciaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Base para tests de integracion HTTP con TestRestTemplate.
 *
 * Levanta la app en un puerto aleatorio con perfil "test" (H2 en memoria).
 * El seeder esta deshabilitado en test, asi que esta clase siembra los datos
 * minimos (permisos, roles, admin, 2 docentes) por repositorio antes de cada test.
 *
 * Limpieza en @AfterEach: borra las tablas en orden inverso a las FKs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Password123*";

    @Autowired protected TestRestTemplate rest;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected RolRepository rolRepository;
    @Autowired protected PermisoRepository permisoRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected CursoRepository cursoRepository;
    @Autowired protected UnidadRepository unidadRepository;
    @Autowired protected SemanaRepository semanaRepository;
    @Autowired protected ClaseRepository claseRepository;
    @Autowired protected MaterialRepository materialRepository;
    @Autowired protected TareaRepository tareaRepository;
    @Autowired protected MatriculaRepository matriculaRepository;
    @Autowired protected EntregaRepository entregaRepository;
    @Autowired protected AsistenciaRepository asistenciaRepository;

    // IDs y tokens utiles para los tests hijos.
    protected UUID adminId;
    protected UUID docenteDuenoId;
    protected UUID docenteAjenoId;
    protected String adminEmail;
    protected String docenteDuenoEmail;
    protected String docenteAjenoEmail;

    protected String adminToken;
    protected String docenteDuenoToken;
    protected String docenteAjenoToken;

    @BeforeEach
    void seedBaseData() {
        limpiarTablas();
        sembrarPermisosYRoles();
        crearUsuariosBase();
        autenticarBase();
    }

    @AfterEach
    void cleanupAfterTest() {
        limpiarTablas();
    }

    // ------------------------------------------------------------------------
    // SEED
    // ------------------------------------------------------------------------

    private void sembrarPermisosYRoles() {
        Set<Permiso> permisos = Arrays.stream(PermisoSistema.values())
                .map(nombre -> {
                    Permiso p = new Permiso();
                    p.setNombre(nombre.name());
                    return permisoRepository.save(p);
                })
                .collect(Collectors.toSet());

        // ADMINISTRADOR: todos los permisos.
        Rol admin = new Rol();
        admin.setNombre("ADMINISTRADOR");
        admin.setDescripcion("Acceso total");
        admin.setPermisos(new HashSet<>(permisos));
        rolRepository.save(admin);

        // DOCENTE: subset igual al seeder de produccion.
        Rol docente = new Rol();
        docente.setNombre("DOCENTE");
        docente.setDescripcion("Gestion academica");
        docente.setPermisos(permisos.stream()
                .filter(p -> Set.of(
                        PermisoSistema.CREAR_TAREA.name(),
                        PermisoSistema.CALIFICAR_TAREA.name(),
                        PermisoSistema.VER_CURSOS.name(),
                        PermisoSistema.ENVIAR_MENSAJES.name()
                ).contains(p.getNombre()))
                .collect(Collectors.toSet()));
        rolRepository.save(docente);

        // ESTUDIANTE: subset igual al seeder de produccion.
        Rol estudiante = new Rol();
        estudiante.setNombre("ESTUDIANTE");
        estudiante.setDescripcion("Acceso estudiantil");
        estudiante.setPermisos(permisos.stream()
                .filter(p -> Set.of(
                        PermisoSistema.VER_CURSOS.name(),
                        PermisoSistema.ENTREGAR_TAREA.name(),
                        PermisoSistema.ENVIAR_MENSAJES.name()
                ).contains(p.getNombre()))
                .collect(Collectors.toSet()));
        rolRepository.save(estudiante);
    }

    private void crearUsuariosBase() {
        adminEmail = "admin.it@kellyacademy.com";
        docenteDuenoEmail = "docente.dueno.it@kellyacademy.com";
        docenteAjenoEmail = "docente.ajeno.it@kellyacademy.com";

        adminId = crearUsuario("Admin", "IT", adminEmail, "ADMINISTRADOR");
        docenteDuenoId = crearUsuario("Docente", "Dueno", docenteDuenoEmail, "DOCENTE");
        docenteAjenoId = crearUsuario("Docente", "Ajeno", docenteAjenoEmail, "DOCENTE");
    }

    protected UUID crearUsuario(String nombre, String apellido, String correo, String rolNombre) {
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado: " + rolNombre));

        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setCorreoElectronico(correo);
        u.setContrasena(passwordEncoder.encode(PASSWORD));
        u.setEstado(EstadoUsuario.ACTIVO);
        u.setRoles(Set.of(rol));
        return usuarioRepository.save(u).getId();
    }

    private void autenticarBase() {
        adminToken = login(adminEmail, PASSWORD);
        docenteDuenoToken = login(docenteDuenoEmail, PASSWORD);
        docenteAjenoToken = login(docenteAjenoEmail, PASSWORD);
    }

    // ------------------------------------------------------------------------
    // HTTP HELPERS
    // ------------------------------------------------------------------------

    protected String login(String correo, String contrasena) {
        AuthRequest req = new AuthRequest();
        req.setCorreoElectronico(correo);
        req.setContrasena(contrasena);

        ResponseEntity<AuthResponse> resp = rest.postForEntity(
                "/auth/login", req, AuthResponse.class
        );
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException(
                    "Login fallo para " + correo + ": " + resp.getStatusCode()
            );
        }
        return resp.getBody().getAccessToken();
    }

    protected HttpHeaders headersConToken(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    protected <T> ResponseEntity<T> post(String url, String token, Object body, Class<T> respType) {
        return rest.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(body, headersConToken(token)),
                respType
        );
    }

    protected <T> ResponseEntity<T> put(String url, String token, Object body, Class<T> respType) {
        return rest.exchange(
                url, HttpMethod.PUT,
                new HttpEntity<>(body, headersConToken(token)),
                respType
        );
    }

    protected <T> ResponseEntity<T> patch(String url, String token, Class<T> respType) {
        return rest.exchange(
                url, HttpMethod.PATCH,
                new HttpEntity<>(headersConToken(token)),
                respType
        );
    }

    protected <T> ResponseEntity<T> get(String url, String token, Class<T> respType) {
        return rest.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(headersConToken(token)),
                respType
        );
    }

    protected ResponseEntity<Void> delete(String url, String token) {
        return rest.exchange(
                url, HttpMethod.DELETE,
                new HttpEntity<>(headersConToken(token)),
                Void.class
        );
    }

    protected <T> ResponseEntity<T> deleteWithBody(String url, String token, Class<T> respType) {
        return rest.exchange(
                url, HttpMethod.DELETE,
                new HttpEntity<>(headersConToken(token)),
                respType
        );
    }

    // ------------------------------------------------------------------------
    // HELPERS DE ESTADO DE CURSO
    // ------------------------------------------------------------------------

    // No hay endpoint administrativo de cambio de estado de curso aun (deuda #13, FASE 5).
    // Los ITs que necesiten curso ACTIVO lo activan por repositorio.
    protected void activarCurso(UUID cursoId) {
        cambiarEstadoCurso(cursoId, EstadoCurso.ACTIVO);
    }

    protected void desactivarCurso(UUID cursoId) {
        cambiarEstadoCurso(cursoId, EstadoCurso.BORRADOR);
    }

    private void cambiarEstadoCurso(UUID cursoId, EstadoCurso estado) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalStateException("Curso no encontrado: " + cursoId));
        curso.setEstado(estado);
        cursoRepository.save(curso);
    }

    // ------------------------------------------------------------------------
    // LIMPIEZA
    // ------------------------------------------------------------------------

    protected void limpiarTablas() {
        // Orden inverso a las FKs: hijos primero.
        asistenciaRepository.deleteAll();
        entregaRepository.deleteAll();
        matriculaRepository.deleteAll();
        tareaRepository.deleteAll();
        materialRepository.deleteAll();
        claseRepository.deleteAll();
        semanaRepository.deleteAll();
        unidadRepository.deleteAll();
        cursoRepository.deleteAll();
        usuarioRepository.deleteAll();
        rolRepository.deleteAll();
        permisoRepository.deleteAll();
    }
}