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
import com.kellyacademy.communication.repository.NotificacionRepository;
import com.kellyacademy.communication.repository.AnuncioRepository;
import com.kellyacademy.communication.repository.ConversacionRepository;
import com.kellyacademy.communication.repository.MensajeRepository;
import com.kellyacademy.calendar.repository.DisponibilidadTutoriaRepository;
import com.kellyacademy.calendar.repository.EventoRepository;
import com.kellyacademy.calendar.repository.TutoriaRepository;
import com.kellyacademy.library.repository.RecursoBibliotecaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.EntityType;
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
import org.springframework.transaction.annotation.Transactional;

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
 * Limpieza en @BeforeEach y @AfterEach: TRUNCATE ... CASCADE descubierto por
 * metamodelo de Hibernate. Ya no hay que tocar esta clase al agregar entidades.
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
    @Autowired protected NotificacionRepository notificacionRepository;
    @Autowired protected AnuncioRepository anuncioRepository;
    @Autowired protected ConversacionRepository conversacionRepository;
    @Autowired protected MensajeRepository mensajeRepository;
    @Autowired protected EventoRepository eventoRepository;
    @Autowired protected DisponibilidadTutoriaRepository disponibilidadTutoriaRepository;
    @Autowired protected TutoriaRepository tutoriaRepository;
    @Autowired protected RecursoBibliotecaRepository recursoBibliotecaRepository;

    @Autowired private EntityManagerFactory entityManagerFactory;

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

    // Cambia el estado de un curso usando el endpoint administrativo real
    // (PATCH /api/cursos/{id}/estado, deuda #13 resuelta). Falla el test si el
    // endpoint devuelve un status distinto a 200.
    protected void activarCurso(UUID cursoId) {
        cambiarEstadoCurso(cursoId, EstadoCurso.ACTIVO);
    }

    // Saca el curso de circulacion via endpoint. Se usa ARCHIVADO (no BORRADOR):
    // una vez que un curso esta ACTIVO, la maquina de estados no permite volver
    // a BORRADOR. ARCHIVADO es la transicion natural de "desactivar" en produccion.
    protected void desactivarCurso(UUID cursoId) {
        cambiarEstadoCurso(cursoId, EstadoCurso.ARCHIVADO);
    }

    private void cambiarEstadoCurso(UUID cursoId, EstadoCurso estado) {
        com.kellyacademy.course.dto.request.CambiarEstadoCursoRequest req =
                new com.kellyacademy.course.dto.request.CambiarEstadoCursoRequest(estado);
        org.springframework.http.ResponseEntity<Object> resp = rest.exchange(
                "/api/cursos/" + cursoId + "/estado",
                org.springframework.http.HttpMethod.PATCH,
                new org.springframework.http.HttpEntity<>(req, headersConToken(adminToken)),
                Object.class
        );
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException(
                    "Fallo cambiar estado del curso " + cursoId + " a " + estado
                            + ": " + resp.getStatusCode()
            );
        }
    }

    // ------------------------------------------------------------------------
    // LIMPIEZA
    // ------------------------------------------------------------------------

    /**
     * Limpia todas las tablas de negocio descubiertas por el metamodelo de Hibernate.
     *
     * Estrategia: TRUNCATE TABLE <tabla> CASCADE. Al usar CASCADE, el orden de las
     * tablas deja de importar (H2 en MODE=PostgreSQL lo soporta). Esto elimina la
     * necesidad de mantener manualmente el orden inverso a las FKs cada vez que se
     * agrega una entidad nueva.
     *
     * Excluye flyway_schema_history (no aplica en test, pero por robustez si algun
     * dia se habilita Flyway en test).
     */
    protected void limpiarTablas() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            // H2 no soporta TRUNCATE ... CASCADE. Se desactivan las FKs temporalmente
            // para poder truncar en cualquier orden, y se restauran al final.
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

            Set<String> tablas = descubrirTablasDeNegocio();
            for (String tabla : tablas) {
                em.createNativeQuery("TRUNCATE TABLE " + tabla).executeUpdate();
            }

            em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();

            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    /**
     * Descubre los nombres fisicos de tabla de todas las entidades JPA registradas.
     * Filtra tablas de infraestructura (Flyway) que no deben tocarse.
     */
    private Set<String> descubrirTablasDeNegocio() {
        Set<String> tablas = entityManagerFactory.getMetamodel().getEntities().stream()
                .map(this::resolverNombreTabla)
                .collect(Collectors.toSet());
        tablas.removeIf(t -> t.equalsIgnoreCase("flyway_schema_history"));
        return tablas;
    }

    /**
     * Resuelve el nombre fisico de tabla de una entidad JPA. Si la entidad no
     * declara @Table, Hibernate infiere el nombre (por defecto: nombre de clase
     * en snake_case, pero eso depende del NamingStrategy). Aqui usamos
     * PersistenceUnitUtil para obtener el nombre ya resuelto por Hibernate.
     */
    private String resolverNombreTabla(EntityType<?> entityType) {
        PersistenceUnitUtil util = entityManagerFactory.getPersistenceUnitUtil();
        // entityType.getName() da el nombre logico de la entidad (clase simple).
        // El nombre fisico de tabla se obtiene del metamodelo de Hibernate via
        // la anotacion @Table o la naming strategy. No hay API JPA estandar
        // limpia para esto, pero el nombre de tabla fisico esta accesible via
        // el metamodelo de Hibernate.
        //
        // Alternativa robusta: usar la anotacion @Table si esta presente, y si no,
        // derivar el nombre con la misma estrategia que Hibernate (CamelCase -> snake_case).
        // Como el proyecto no configura una naming strategy custom, Hibernate usa
        // CamelCaseToUnderscoresNamingStrategy por defecto en Spring Boot.
        Class<?> clase = entityType.getJavaType();
        jakarta.persistence.Table table = clase.getAnnotation(jakarta.persistence.Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return camelCaseASnakeCase(clase.getSimpleName());
    }

    private String camelCaseASnakeCase(String nombre) {
        return nombre
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .toLowerCase();
    }
}