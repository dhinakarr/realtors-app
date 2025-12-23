package com.realtors.sitevisit.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import com.realtors.sitevisit.dto.AgentDto;

public class AgentRowMapper implements RowMapper<AgentDto> {

    @Override
    public AgentDto mapRow(ResultSet rs, int rowNum) throws SQLException {

    	AgentDto dto = new AgentDto();
        dto.setAgentId(UUID.fromString(rs.getString("user_id")));
        dto.setAgentName(rs.getString("full_name"));
        return dto;
    }

}
