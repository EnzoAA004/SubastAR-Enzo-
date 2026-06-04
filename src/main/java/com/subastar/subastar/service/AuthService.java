package com.subastar.subastar.service;

import com.subastar.subastar.dto.auth.*;
import com.subastar.subastar.dto.usuario.UsuarioResumen;
import com.subastar.subastar.exception.BadRequestException;
import com.subastar.subastar.exception.ConflictException;
import com.subastar.subastar.exception.ForbiddenException;
import com.subastar.subastar.exception.GoneException;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.exception.UnauthorizedException;
import com.subastar.subastar.model.*;
import com.subastar.subastar.repository.*;
import com.subastar.subastar.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String RECUPERACION_MESSAGE =
            "Si el email existe, enviamos instrucciones para recuperar la contraseña.";

    private final RegistroPendienteRepository registroPendienteRepository;
    private final CredencialRepository credencialRepository;
    private final PersonaRepository personaRepository;
    private final ClienteRepository clienteRepository;
    private final PaisRepository paisRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void registroStep1(RegistroStep1Request req, MultipartFile dniFrente, MultipartFile dniDorso) {
        String email = req.getEmail() != null ? req.getEmail().trim() : null;
        log.info("Inicio de registro");
        log.info("Email recibido para registro: {}", email);

        if (credencialRepository.existsByEmail(email)) {
            log.info("Registro rechazado: email ya existe como credencial: {}", email);
            throw new ConflictException("El email ya está registrado");
        }
        if (registroPendienteRepository.existsByEmail(email)) {
            log.info("Registro rechazado: email ya existe como registro pendiente: {}", email);
            throw new ConflictException("Ya hay un registro pendiente para este email. Podés reenviar el código o cancelar el registro pendiente.");
        }

        Pais pais = paisRepository.findAll().stream()
                .filter(p -> (p.getNombre() != null && p.getNombre().equalsIgnoreCase(req.getPaisOrigen()))
                        || (p.getNombreCorto() != null && p.getNombreCorto().equalsIgnoreCase(req.getPaisOrigen())))
                .findFirst()
                .orElseThrow(() -> {
                    log.info("País no encontrado en registro: {}", req.getPaisOrigen());
                    return new BadRequestException("País no encontrado: " + req.getPaisOrigen());
                });
        log.info("País encontrado para registro: {} ({})", pais.getNombre(), pais.getNumero());

        RegistroPendiente registro = new RegistroPendiente();
        registro.setEmail(email);
        registro.setNombre(req.getNombre());
        registro.setApellido(req.getApellido());
        registro.setDomicilio(req.getDomicilio());
        registro.setPaisNumero(pais.getNumero());
        registro.setEstado("pendiente_revision");

        try {
            if (dniFrente != null && !dniFrente.isEmpty()) registro.setFotoDniFrente(dniFrente.getBytes());
            if (dniDorso != null && !dniDorso.isEmpty()) registro.setFotoDniDorso(dniDorso.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Error al procesar las imágenes del DNI");
        }

        String codigo = generarCodigoRegistro();
        registro.setCodigoVerificacion(codigo);
        registro.setCodigoExpiresAt(LocalDateTime.now().plusHours(24));
        registro.setEstado("aprobado");
        registroPendienteRepository.save(registro);
        log.info("RegistroPendiente creado para email: {}", email);

        try {
            emailService.enviarNotificacionAprobacion(email, codigo);
            log.info("Email de registro enviado a: {}", email);
        } catch (MailException e) {
            log.error("Error de mail al enviar código de registro a {}", email, e);
            throw new BadRequestException("No se pudo enviar el email de verificación. Intentá nuevamente.");
        }
    }

    @Transactional
    public String verificarCodigo(VerificarCodigoRequest req) {
        RegistroPendiente registro = registroPendienteRepository
                .findByEmailAndEstado(req.getEmail(), "aprobado")
                .orElseThrow(() -> new ResourceNotFoundException("Email no encontrado en el sistema"));

        if (registro.getCodigoExpiresAt() != null && LocalDateTime.now().isAfter(registro.getCodigoExpiresAt())) {
            throw new GoneException("El código expiró");
        }
        if (!req.getCodigo().equals(registro.getCodigoVerificacion())) {
            throw new BadRequestException("Código inválido o malformado");
        }

        String token = UUID.randomUUID().toString();
        registro.setTokenVerificacion(token);
        registro.setTokenExpiresAt(LocalDateTime.now().plusHours(2));
        registroPendienteRepository.save(registro);
        return token;
    }

    @Transactional
    public LoginResponse completarRegistro(RegistroStep2Request req) {
        RegistroPendiente registro = registroPendienteRepository
                .findByTokenVerificacion(req.getTokenVerificacion())
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificación no encontrado"));

        if (registro.getTokenExpiresAt() != null && LocalDateTime.now().isAfter(registro.getTokenExpiresAt())) {
            throw new BadRequestException("Token de verificación expirado");
        }
        if (!req.getPassword().equals(req.getPasswordConfirmacion())) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }
        if (credencialRepository.existsByEmail(registro.getEmail())) {
            throw new ConflictException("La cuenta ya fue creada.");
        }

        Persona persona = new Persona();
        persona.setNombre(registro.getNombre() + " " + registro.getApellido());
        persona.setDocumento("PENDIENTE");
        persona.setDireccion(registro.getDomicilio());
        persona.setEstado("activo");
        persona.setFoto(registro.getFotoDniFrente());
        persona = personaRepository.save(persona);

        Empleado verificador = empleadoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No hay empleados verificadores disponibles. Contacte al administrador."));

        Cliente cliente = new Cliente();
        cliente.setIdentificador(persona.getIdentificador());
        cliente.setAdmitido("si");
        cliente.setCategoria("comun");
        cliente.setVerificadorId(verificador.getIdentificador());
        Pais pais = paisRepository.findById(registro.getPaisNumero()).orElse(null);
        if (pais != null) cliente.setPais(pais);
        clienteRepository.save(cliente);

        Credencial credencial = new Credencial();
        credencial.setPersonaId(persona.getIdentificador());
        credencial.setEmail(registro.getEmail());
        credencial.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        credencialRepository.save(credencial);

        registroPendienteRepository.delete(registro);
        return generarLoginResponse(credencial, cliente, persona);
    }

    public LoginResponse login(LoginRequest req) {
        Credencial credencial = credencialRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email o contraseña incorrectos"));

        if (!passwordEncoder.matches(req.getPassword(), credencial.getPasswordHash())) {
            throw new UnauthorizedException("Email o contraseña incorrectos");
        }

        Cliente cliente = clienteRepository.findById(credencial.getPersonaId())
                .orElseThrow(() -> new UnauthorizedException("Email o contraseña incorrectos"));

        if ("bloqueado".equals(estadoCliente(cliente))) {
            throw new ForbiddenException("Cuenta bloqueada o inactiva");
        }

        return generarLoginResponse(credencial, cliente, credencial.getPersona());
    }

    @Transactional
    public void reenviarCodigoRegistro(ReenviarCodigoRegistroRequest req) {
        String email = req.getEmail().trim();
        if (credencialRepository.existsByEmail(email)) {
            throw new ConflictException("La cuenta ya fue creada.");
        }

        RegistroPendiente registro = registroPendienteRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No hay registro pendiente para este email."));

        String codigo = generarCodigoRegistro();
        registro.setCodigoVerificacion(codigo);
        registro.setCodigoExpiresAt(LocalDateTime.now().plusHours(24));
        registro.setTokenVerificacion(null);
        registro.setTokenExpiresAt(null);
        registro.setEstado("aprobado");
        registroPendienteRepository.save(registro);

        try {
            emailService.enviarNotificacionAprobacion(email, codigo);
            log.info("Código de registro reenviado a: {}", email);
        } catch (MailException e) {
            log.error("Error de mail al reenviar código de registro a {}", email, e);
            throw new BadRequestException("No se pudo reenviar el código. Intentá nuevamente.");
        }
    }

    @Transactional
    public void cancelarRegistroPendiente(CancelarRegistroPendienteRequest req) {
        String email = req.getEmail().trim();
        if (credencialRepository.existsByEmail(email)) {
            throw new ConflictException("La cuenta ya fue creada.");
        }

        RegistroPendiente registro = registroPendienteRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No hay registro pendiente para este email."));
        registroPendienteRepository.delete(registro);
        log.info("Registro pendiente eliminado para email: {}", email);
    }

    @Transactional
    public String recuperarPassword(RecuperarPasswordRequest req) {
        String email = req.getEmail().trim();
        Credencial credencial = credencialRepository.findByEmail(email).orElse(null);
        if (credencial == null) {
            return RECUPERACION_MESSAGE;
        }

        invalidarPasswordResetTokensActivos(email);

        String codigo = generarCodigoSeisDigitos();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(email);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setCodigoHash(passwordEncoder.encode(codigo));
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        passwordResetTokenRepository.save(resetToken);

        try {
            emailService.enviarCodigoRecuperacionPassword(email, codigo);
            log.info("Email de recuperación de contraseña enviado a: {}", email);
        } catch (MailException e) {
            log.error("Error de mail al enviar recuperación de contraseña a {}", email, e);
            throw new BadRequestException("No se pudo enviar el email de recuperación. Intentá nuevamente.");
        }

        return RECUPERACION_MESSAGE;
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void confirmarRecuperarPassword(ConfirmarRecuperarPasswordRequest req) {
        String email = req.getEmail().trim();
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findTopByEmailAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("Código inválido o expirado."));

        if (LocalDateTime.now().isAfter(resetToken.getExpiresAt())) {
            throw new GoneException("El código expiró.");
        }
        if (resetToken.getAttempts() >= 5) {
            throw new BadRequestException("Demasiados intentos. Pedí un nuevo código.");
        }
        if (!passwordEncoder.matches(req.getCodigo(), resetToken.getCodigoHash())) {
            resetToken.setAttempts(resetToken.getAttempts() + 1);
            passwordResetTokenRepository.save(resetToken);
            throw new BadRequestException("Código inválido.");
        }
        if (!req.getPassword().equals(req.getPasswordConfirmacion())) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }

        Credencial credencial = credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        credencial.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        credencialRepository.save(credencial);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        log.info("Contraseña actualizada por recuperación para email: {}", email);
    }

    @Transactional
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (token != null && jwtUtil.isTokenValid(token)) {
            TokenBlacklist blacklist = new TokenBlacklist();
            blacklist.setToken(token);
            blacklist.setExpiresAt(jwtUtil.extractExpiration(token));
            tokenBlacklistRepository.save(blacklist);
        }
    }

    private void invalidarPasswordResetTokensActivos(String email) {
        List<PasswordResetToken> activos = passwordResetTokenRepository
                .findByEmailAndUsedAtIsNullAndInvalidatedAtIsNull(email);
        LocalDateTime now = LocalDateTime.now();
        activos.forEach(token -> token.setInvalidatedAt(now));
        passwordResetTokenRepository.saveAll(activos);
    }

    private String generarCodigoRegistro() {
        return String.format("%04d", SECURE_RANDOM.nextInt(10000));
    }

    private String generarCodigoSeisDigitos() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    private String estadoCliente(Cliente cliente) {
        if (!"si".equals(cliente.getAdmitido())) return "bloqueado";
        return "activo";
    }

    private LoginResponse generarLoginResponse(Credencial credencial, Cliente cliente, Persona persona) {
        String token = jwtUtil.generateToken(credencial.getEmail(), cliente.getIdentificador(), cliente.getCategoria());
        UsuarioResumen resumen = buildResumen(cliente, credencial.getEmail(), persona);
        return new LoginResponse(token, "Bearer", resumen);
    }

    private UsuarioResumen buildResumen(Cliente cliente, String email, Persona persona) {
        UsuarioResumen r = new UsuarioResumen();
        r.setId(cliente.getIdentificador());
        if (persona != null && persona.getNombre() != null) {
            String[] parts = persona.getNombre().split(" ", 2);
            r.setNombre(parts[0]);
            r.setApellido(parts.length > 1 ? parts[1] : "");
        }
        r.setEmail(email);
        r.setCategoria(cliente.getCategoria() != null ? cliente.getCategoria() : "comun");
        r.setEstado(estadoCliente(cliente));
        return r;
    }
}
