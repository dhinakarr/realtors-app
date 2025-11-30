package com.realtors.admin.service;

import com.realtors.admin.dto.UploadTestDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

@Service
public class UploadTestService {

    private final JdbcTemplate jdbcTemplate;

    public UploadTestService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // CREATE USER
    public UUID createUser(UploadTestDto dto, MultipartFile profileImage) throws Exception {
        String sql = """
            INSERT INTO upload_test (email, mobile, full_name, address, profile_image, meta)
            VALUES (?, ?, ?, ?, ?, ?::jsonb)
            RETURNING user_id
            """;

        // ← ADD THIS LINE (the trick!)
        final byte[] imageBytes = (profileImage != null && !profileImage.isEmpty())
                ? profileImage.getBytes()
                : null;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dto.getEmail());
            ps.setString(2, dto.getMobile());
            ps.setString(3, dto.getFullName());
            ps.setString(4, dto.getAddress());

            // Now use the final variable → works perfectly
            if (imageBytes != null) {
                ps.setBytes(5, imageBytes);
            } else {
                ps.setNull(5, java.sql.Types.BINARY);
            }

            String meta = dto.getMetaJson() != null ? dto.getMetaJson() : "{}";
            ps.setString(6, meta);

            return ps;
        }, keyHolder);

        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("user_id")) {
            return (UUID) keys.get("user_id");
        }
        throw new RuntimeException("Failed to create user – no user_id returned");
    }

    // FIND BY ID
    public UploadTestDto findById(UUID userId) {
        String sql = "SELECT * FROM upload_test WHERE user_id = ?";

        return jdbcTemplate.query(sql, new Object[]{userId}, rs -> {
            if (rs.next()) {
                UploadTestDto dto = new UploadTestDto();
                dto.setUserId((UUID) rs.getObject("user_id"));
                dto.setEmail(rs.getString("email"));
                dto.setMobile(rs.getString("mobile"));
                dto.setFullName(rs.getString("full_name"));
                dto.setAddress(rs.getString("address"));
                dto.setProfileImage(rs.getBytes("profile_image")); // BYTEA → byte[]

                // Handle JSONB
                Object meta = rs.getObject("meta");
                dto.setMetaJson(meta != null ? meta.toString() : "{}");

                return dto;
            }
            return null;
        });
    }

    // UPDATE PROFILE IMAGE + META PATCH
    public boolean updateProfileImage(UUID userId, MultipartFile image, String additionalMeta) throws Exception {
        // ← Fix here too
        final byte[] imageBytes = (image != null && !image.isEmpty()) ? image.getBytes() : null;

        String sql = """
            UPDATE upload_test 
            SET profile_image = ?, 
                meta = meta || ?::jsonb 
            WHERE user_id = ?
            """;

        int rows = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);

            if (imageBytes != null) {
                ps.setBytes(1, imageBytes);
            } else {
                ps.setNull(1, java.sql.Types.BINARY);
            }

            String metaPatch = (additionalMeta != null && !additionalMeta.trim().isEmpty())
                    ? additionalMeta : "{}";
            ps.setString(2, metaPatch);
            ps.setObject(3, userId);

            return ps;
        });

        return rows > 0;
    }
}