package com.nubixconta.modules.administration.service;

import com.nubixconta.modules.administration.dto.user.UserResponseNameDTO;
import com.nubixconta.modules.administration.entity.AccessLog;
import com.nubixconta.modules.administration.entity.User; // Importa User
import com.nubixconta.modules.administration.repository.AccessLogRepository;
import com.nubixconta.modules.administration.repository.UserRepository; // Importa UserRepository
import com.nubixconta.modules.administration.dto.AccessLog.AccessLogResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccessLogService {

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Obtiene registros de acceso filtrados por usuario, rango de fechas, o ambos.
     * Los parámetros son opcionales y se pueden combinar.
     *
     * @param userId El ID del usuario. Opcional.
     * @param startDate La fecha de inicio del rango. Opcional.
     * @param endDate La fecha de fin del rango. Opcional.
     * @return Una lista de AccessLogResponseDTO filtrada.
     */

    public List<AccessLogResponseDTO> getFilteredAccessLogs(Integer userId, LocalDate startDate, LocalDate endDate) {
        List<AccessLog> accessLogs;

        // Definimos el objeto Sort para ordenar por 'dateStart' de forma descendente.
        Sort sortByDateStartDesc = Sort.by("dateStart").descending();

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

        if (userId != null && startDate != null && endDate != null) {
            // Se usa el método modificado del repositorio con el Sort.
            accessLogs = accessLogRepository.findByUser_IdAndDateStartBetween(userId, startDateTime, endDateTime, sortByDateStartDesc);
        } else if (userId != null) {
            // Se usa el método modificado del repositorio con el Sort.
            accessLogs = accessLogRepository.findByUser_Id(userId, sortByDateStartDesc);
        } else if (startDate != null && endDate != null) {
            // Se usa el método modificado del repositorio con el Sort.
            accessLogs = accessLogRepository.findByDateStartBetween(startDateTime, endDateTime, sortByDateStartDesc);
        } else {
            // Si no hay filtros, se usa findAll() que también acepta un Sort.
            accessLogs = accessLogRepository.findAll(sortByDateStartDesc);
        }

        return toDTOList(accessLogs);
    }



    /**
     * Registra el inicio de sesión de un usuario.
     * @param userId El ID del usuario que inicia sesión.
     * @return El AccessLogResponseDTO del registro creado.
     */
    public AccessLogResponseDTO recordLogin(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario con ID " + userId + " no encontrado."));

        AccessLog accessLog = new AccessLog(user, LocalDateTime.now());
        accessLog.setDateEnd(null); //  date_end sera nulo al inicio
        AccessLog savedLog = accessLogRepository.save(accessLog);

        // Mapea la entidad guardada al DTO de respuesta
        return modelMapper.map(savedLog, AccessLogResponseDTO.class);
    }

    /**
     * Registra el cierre de sesión de un usuario, actualizando el registro de inicio de sesión.
     * @param accessLogId El ID del registro de AccessLog a actualizar.
     * @param currentUserId El ID del usuario para validar la propiedad del log (obtenido del token).
     */
    public void recordLogout(Long accessLogId, Integer currentUserId) {
        Optional<AccessLog> accessLogOptional = accessLogRepository.findById(accessLogId);

        if (accessLogOptional.isPresent()) {
            AccessLog accessLog = accessLogOptional.get();


            if (!accessLog.getUser().getId().equals(currentUserId)) { // Compara el ID del User del log con el ID del token
                throw new SecurityException("Intento de cerrar sesión de un registro no autorizado para el usuario " + currentUserId);
            }

            if (accessLog.getDateEnd() != null) {
                return;
            }

            accessLog.setDateEnd(LocalDateTime.now()); // Usa date_end
            accessLogRepository.save(accessLog);
        } else {
            throw new RuntimeException("Registro de acceso con ID " + accessLogId + " no encontrado.");
        }
    }

    /**
     * Busca el último registro de inicio de sesión activo (sin fecha de fin) para un usuario.
     * @param userId El ID del usuario.
     * @return El AccessLog si se encuentra una sesión activa.
     */
    public Optional<AccessLog> findPendingAccessLogForUser(Integer userId) {
        return accessLogRepository.findTopByUser_IdAndDateEndIsNullOrderByDateStartDesc(userId);
    }

    //Este metodo separa en fecha y hora
    public AccessLogResponseDTO extractDateAndTime(AccessLog accessLog) {
        AccessLogResponseDTO dto = new AccessLogResponseDTO();
        dto.setId(accessLog.getId() != null ? accessLog.getId().longValue() : null);


        if (accessLog.getUser() != null) {
            dto.setUser(this.toUserResponseDTO(accessLog.getUser()));
        }

        // Separar dateStart en fecha y hora
        if (accessLog.getDateStart() != null) {
            dto.setDateStartDate(accessLog.getDateStart().toLocalDate());
            dto.setDateStartTime(accessLog.getDateStart().toLocalTime());
        }

        // Separar dateEnd en fecha y hora, si no es nulo
        if (accessLog.getDateEnd() != null) {
            dto.setDateEndDate(accessLog.getDateEnd().toLocalDate());
            dto.setDateEndTime(accessLog.getDateEnd().toLocalTime());
        }

        return dto;
    }

    public List<AccessLogResponseDTO> toDTOList(List<AccessLog> accessLogs) {
        return accessLogs.stream()
                .map(this::extractDateAndTime)
                .collect(Collectors.toList());
    }
    //Metodo que regresa el nombre y apellido del usuario
    private UserResponseNameDTO toUserResponseDTO(User user) {
        UserResponseNameDTO userDTO = new UserResponseNameDTO();
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        return userDTO;
    }

    public List<AccessLogResponseDTO> getAllAccessLogs() {
        List<AccessLog> accessLogs = accessLogRepository.findAll();
        return toDTOList(accessLogs);
    }

}