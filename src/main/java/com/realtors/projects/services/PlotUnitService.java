package com.realtors.projects.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.realtors.admin.service.AbstractBaseService;
import com.realtors.projects.dto.PlotUnitDto;
import com.realtors.projects.repository.PlotUnitRepository;

import java.util.*;

@Service
public class PlotUnitService extends AbstractBaseService<PlotUnitDto, UUID>{

    private final PlotUnitRepository repo;
    private JdbcTemplate jdbcTemplate;
    
    public PlotUnitService(PlotUnitRepository repo, JdbcTemplate jdbcTemplate) {
    	super(PlotUnitDto.class, "plot_units", jdbcTemplate);
    	this.repo = repo;
    }
    
    @Override
    protected String getIdColumn() {
        return "feature_id";
    }

    public PlotUnitDto createPlot(PlotUnitDto dto) {
        return super.create(dto);
    }

    public List<PlotUnitDto> getByProject(UUID projectId) {
        return repo.findByProjectId(projectId);
    }
    
    public PlotUnitDto patchUpdate(UUID plotId, Map<String, Object> partialData) {
    	return super.patch(plotId, partialData);
    }

    public void delete(UUID id) {
        //repo.delete(id);
    	super.softDelete(id);
    }

    public int update(PlotUnitDto dto) {
        return repo.update(dto);
    }

    // ---------------------------------------------------
    // AUTO-GENERATE PLOTS
    // ---------------------------------------------------
    public void generatePlots(UUID projectId, int numberOfPlots, int startNumber) {

        List<PlotUnitDto> list = new ArrayList<>();

        for (int i = 0; i < numberOfPlots; i++) {
            PlotUnitDto dto = new PlotUnitDto();
            dto.setPlotId(UUID.randomUUID());
            dto.setProjectId(projectId);
            dto.setPlotNumber(String.valueOf(startNumber + i));
            dto.setStatus("AVAILABLE");
            dto.setIsPrime(false);

            list.add(dto);
        }

        repo.bulkInsert(list);
    }
}

