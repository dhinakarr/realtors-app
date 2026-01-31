package com.realtors.projects.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.AbstractBaseService;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.AppUtil;
import com.realtors.projects.dto.PlotDetailsDto;
import com.realtors.projects.dto.PlotUnitDto;
import com.realtors.projects.dto.PlotUnitStatus;
import com.realtors.projects.dto.ProjectDto;
import com.realtors.projects.repository.PlotUnitRepository;
import com.realtors.projects.repository.ProjectRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PlotUnitService extends AbstractBaseService<PlotUnitDto, UUID>{

    private final PlotUnitRepository repo;
    private final PlotPricingService pricingService;
    private ProjectRepository projectRepository;
    private JdbcTemplate jdbcTemplate;
    private final AuditTrailService audit;
    private String TABLE_NAME="plot_units";
    
    public PlotUnitService(PlotUnitRepository repo, JdbcTemplate jdbcTemplate, 
    		PlotPricingService pricingService, ProjectRepository projectRepository, AuditTrailService audit) {
    	super(PlotUnitDto.class, "plot_units", jdbcTemplate);
    	this.repo = repo;
    	this.pricingService = pricingService;
    	this.projectRepository = projectRepository;
    	this.audit = audit;
    }
    
    @Override
    protected String getIdColumn() {
        return "plot_id";
    }
    
    /** ✅ Update user form response */
    public EditResponseDto<PlotUnitDto> editFormResponse(UUID plotId) {
//    	logger.info("@ProjectService.getEditForm UUID id: "+projectId);
    	DynamicFormResponseDto form = super.buildDynamicFormResponse();
    	if (plotId ==null) {
    		return new EditResponseDto<>(null, form);
    	}
        PlotUnitDto opt = super.findById(plotId).stream().findFirst().get();
        
        EditResponseDto<PlotUnitDto> result = new EditResponseDto(opt, form);
        return  result;
    }

    public PlotUnitDto createPlot(PlotUnitDto dto) {
    	audit.auditAsync(TABLE_NAME, dto.getPlotId(), EnumConstants.CREATE);
        return super.create(dto);
    }

    public List<PlotUnitDto> getByProject(UUID projectId) {
        return repo.findByProjectId(projectId);
    }
    
    public PlotDetailsDto getDetailsPlotId(UUID plotId) {
    	PlotDetailsDto detailDto = new PlotDetailsDto();
    	PlotUnitDto plotDto = super.findById(plotId).get();
    	
    	ProjectDto projectDto = projectRepository.findById(plotDto.getProjectId());
    	detailDto.setDocumentationCharges(projectDto.getDocCharges());
    	detailDto.setOtherCharges(projectDto.getOtherCharges());
    	if (plotDto.getRatePerSqft() == null)
    		detailDto.setRatePerSqft(projectDto.getPricePerSqft());
    	detailDto.setPlotData(plotDto);
    	return detailDto;
    }
    
    public PlotUnitDto getByPlotId(UUID plotId) {
    	return super.findById(plotId).get();
    }
    
    public PlotUnitDto patchUpdate(UUID plotId, Map<String, Object> partialData) {
    	PlotUnitDto plotDto = getByPlotId(plotId);
    	ProjectDto projectDto = projectRepository.findById(plotDto.getProjectId());
    	
    	Object areaObj = partialData.get("area");
    	BigDecimal area = areaObj != null ? new BigDecimal(areaObj.toString()) : null;
    	
    	Object basePriceObj = partialData.get("ratePerSqft");
    	BigDecimal sqftRate = projectDto.getPricePerSqft();

    	Boolean isPrime = Boolean.TRUE.equals(partialData.get("isPrime"));
    	if (isPrime && basePriceObj != null) {
    	    sqftRate = new BigDecimal(basePriceObj.toString());
    	} 
    	
        BigDecimal basePrice = pricingService.calculateBasePrice(area, sqftRate);
        BigDecimal stampDuty = AppUtil.percent(AppUtil.nz(projectDto.getRegCharges()));
        
        BigDecimal guideline = area.multiply(AppUtil.nz(projectDto.getGuidanceValue()));
    	BigDecimal registrationCharges = guideline.multiply(stampDuty).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = basePrice.add(registrationCharges)
        											.add(AppUtil.nz(projectDto.getDocCharges()))
        											.add(AppUtil.nz(projectDto.getOtherCharges()));
    	
    	partialData.put("area", area);
    	partialData.put("ratePerSqft", sqftRate);
    	partialData.put("basePrice", basePrice);
    	partialData.put("registrationCharges", registrationCharges);
    	partialData.put("totalPrice", totalPrice);
    	partialData.put("isPrime", isPrime);
    	audit.auditAsync(TABLE_NAME, plotId, EnumConstants.PATCH);
    	return super.patch(plotId, partialData);
    }
    
    public PlotUnitDto updateCancel(UUID plotId, Map<String, Object> partialData) {
    	audit.auditAsync(TABLE_NAME, plotId, EnumConstants.UPDATE);
    	return super.patch(plotId, partialData);
    }

    public void delete(UUID id) {
    	audit.auditAsync(TABLE_NAME, id, EnumConstants.DELETE);
        repo.delete(id);
    	//super.softDelete(id);
    }
    
    public boolean deleteByProjectId(UUID projectId) {
    	audit.auditAsync(TABLE_NAME, projectId, EnumConstants.DELETE);
        return repo.deleteByProjectId(projectId);
    }

    public int update(PlotUnitDto dto) {
    	audit.auditAsync(TABLE_NAME, dto.getPlotId(), EnumConstants.UPDATE);
        return repo.update(dto);
    }

    // ---------------------------------------------------
    // AUTO-GENERATE PLOTS
    // ---------------------------------------------------
    public void generatePlots(UUID projectId, List<String> plotNumbers) {
        List<PlotUnitDto> list = new ArrayList<>();
        if (plotNumbers != null && !plotNumbers.isEmpty()) {
        	for(String plotNo: plotNumbers) {
        		PlotUnitDto dto = new PlotUnitDto();
                dto.setPlotId(UUID.randomUUID());
                dto.setProjectId(projectId);
                dto.setPlotNumber(plotNo);
                dto.setStatus("AVAILABLE");
                dto.setIsPrime(false);
                list.add(dto);
        	}
        	repo.bulkInsert(list);
        }
    }
    
   public PlotUnitStatus getPlotStat(UUID projectId) {
	   return repo.getPlotStats(projectId).getFirst();
   }
}

