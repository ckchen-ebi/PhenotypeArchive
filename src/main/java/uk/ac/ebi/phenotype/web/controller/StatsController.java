/**
 * Copyright © 2011-2013 EMBL - European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License.  
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.phenotype.web.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;

import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.ebi.generic.util.JSONRestUtil;
import uk.ac.ebi.phenotype.dao.BiologicalModelDAO;
import uk.ac.ebi.phenotype.dao.CategoricalStatisticsDAO;
import uk.ac.ebi.phenotype.dao.GenomicFeatureDAO;
import uk.ac.ebi.phenotype.dao.PhenotypeCallSummaryDAO;
import uk.ac.ebi.phenotype.dao.PhenotypePipelineDAO;
import uk.ac.ebi.phenotype.dao.TimeSeriesStatisticsDAO;
import uk.ac.ebi.phenotype.dao.UnidimensionalStatisticsDAO;
import uk.ac.ebi.phenotype.data.impress.Utilities;
import uk.ac.ebi.phenotype.error.GenomicFeatureNotFoundException;
import uk.ac.ebi.phenotype.pojo.BiologicalModel;
import uk.ac.ebi.phenotype.pojo.GenomicFeature;
import uk.ac.ebi.phenotype.pojo.ObservationType;
import uk.ac.ebi.phenotype.pojo.Parameter;
import uk.ac.ebi.phenotype.pojo.ParameterIncrement;
import uk.ac.ebi.phenotype.pojo.PhenotypeCallSummary;
import uk.ac.ebi.phenotype.pojo.Pipeline;
import uk.ac.ebi.phenotype.stats.ChartData;
import uk.ac.ebi.phenotype.stats.ChartType;
import uk.ac.ebi.phenotype.stats.ExperimentDTO;
import uk.ac.ebi.phenotype.stats.JSONGraphUtils;
import uk.ac.ebi.phenotype.stats.ObservationService;
import uk.ac.ebi.phenotype.stats.PipelineProcedureData;
import uk.ac.ebi.phenotype.stats.PipelineProcedureTablesCreator;
import uk.ac.ebi.phenotype.stats.TableObject;
import uk.ac.ebi.phenotype.stats.categorical.CategoricalChartAndTableProvider;
import uk.ac.ebi.phenotype.stats.categorical.CategoricalResultAndCharts;
import uk.ac.ebi.phenotype.stats.timeseries.TimeSeriesChartAndTableProvider;
import uk.ac.ebi.phenotype.stats.unidimensional.UnidimensionalChartAndTableProvider;
import uk.ac.ebi.phenotype.stats.unidimensional.UnidimensionalDataSet;

@Controller
public class StatsController implements BeanFactoryAware {

	private final Logger log = LoggerFactory.getLogger(StatsController.class);
	private BeanFactory bf;

	@Autowired
	private BiologicalModelDAO bmDAO;

	@Autowired
	private PhenotypePipelineDAO pipelineDAO;

	@Autowired
	private GenomicFeatureDAO genesDao;

	@Autowired
	private CategoricalStatisticsDAO categoricalStatsDao;
	
	@Autowired
	private UnidimensionalStatisticsDAO unidimensionalStatisticsDAO;
	
	@Autowired
	private TimeSeriesStatisticsDAO timeSeriesStatisticsDAO;

	@Autowired
	private PhenotypeCallSummaryDAO phenoDAO;
	@Autowired
	private CategoricalChartAndTableProvider categoricalChartAndTableProvider;
	@Autowired
	private TimeSeriesChartAndTableProvider timeSeriesChartAndTableProvider;
	@Autowired
	private UnidimensionalChartAndTableProvider continousChartAndTableProvider;
	@Autowired
	private PhenotypeCallSummaryDAO phenotypeCallSummaryDAO;
	
	@Autowired
	private ObservationService observationService;
		
	/**
	 * Runs when the request missing an accession ID. This redirects to the
	 * search page which defaults to showing all genes in the list
	 */
	@RequestMapping("/stats")
	public String rootForward() {
		return "redirect:/search";
	}

	@RequestMapping("/stats/genes/{acc}")
	public String generateProcedureView(
			@RequestParam(required = false, /*defaultValue = "ESLIM_001_001_007",*/ value = "parameterId") String[] parameterIds,
			@RequestParam(required = false, value = "gender") String[] gender,
			@RequestParam(required = false, value = "zygosity") String[] zygosity,
			@RequestParam(required = false, value = "model") String[] biologicalModelsParam,
			@PathVariable String acc, Model model)
			throws GenomicFeatureNotFoundException, IOException, URISyntaxException, SolrServerException {

		boolean statsError=false;
		// Get the global application configuration
		@SuppressWarnings("unchecked")
		Map<String, String> config = (Map<String, String>) bf
				.getBean("globalConfiguration");
		GenomicFeature gene = genesDao.getGenomicFeatureByAccession(acc);
		if (gene == null) {
			throw new GenomicFeatureNotFoundException("Gene " + acc
					+ " can't be found.", acc);
		}
		
		
			 
		//
		log.info(gene.toString());
		model.addAttribute("gene", gene);

		List<String> paramIds = getParamsAsList(parameterIds);
		List<String> genderList = getParamsAsList(gender);
		List<String> zyList=getParamsAsList(zygosity);
		List<String> biologicalModelsParams=getParamsAsList(biologicalModelsParam);
		
		//if no parameter Ids or gender or zygosity or bm specified then create a procedure view by default 
		if(paramIds.isEmpty() && genderList.isEmpty() && zyList.isEmpty() && biologicalModelsParams.isEmpty()){
			generateProcedureView(acc, model, gene);
			return "procedures";
		}
		
		

		// MGI:105313 male het param 655
		// log.info("acc=" + acc);
		List<JSONObject> charts = new ArrayList<JSONObject>();
		List<UnidimensionalDataSet> allUnidimensionalChartsAndTables=new ArrayList<UnidimensionalDataSet>();
		List<CategoricalResultAndCharts> allCategoricalResultAndCharts=new ArrayList<CategoricalResultAndCharts>();
		List<TableObject> categoricalTables=new ArrayList<TableObject>();
		List<BiologicalModel> categoricalMutantBiologicalModels=new ArrayList<BiologicalModel>();
		List<BiologicalModel> unidimensionalMutantBiologicalModels=new ArrayList<BiologicalModel>();
		List<BiologicalModel> timeSeriesMutantBiologicalModels=new ArrayList<BiologicalModel>();
		List<ChartData> timeSeriesChartsAndTables=new ArrayList<ChartData>();
		// param 655
		// female homzygote
		// population id=4640 or 4047 - male, het.
		
		
		
			
		for (String parameterId : paramIds) {
			//make a solr request for each param?
			 //get all the experimental data for this param and gene
			 
			 //using this request only to get observation type so can I just get this from the db instead as getting param for db anyway- how did it work before?
//			 net.sf.json.JSONObject expResult=JSONGraphUtils.getExperimentalData(parameterId, acc, config);
//			 
//			 JSONArray resultsArray=JSONRestUtil.getDocArray(expResult);
//			 if(resultsArray.size()>0) {
//				 net.sf.json.JSONObject exp=resultsArray.getJSONObject(0);
//				 observationTypeForParam=ObservationType.valueOf(exp.getString("observationType"));
//			 }
			
			Parameter parameter = pipelineDAO.getParameterByStableIdAndVersion(parameterId, 1, 0);
			String[] parameterUnits=parameter.checkParameterUnits();
			String xUnits="";
			String yUnits="";
			
			if(parameterUnits.length>0){
			 xUnits=parameterUnits[0];
			}
			if(parameterUnits.length>1){
				yUnits=parameterUnits[1];
			}
			ObservationType observationTypeForParam=Utilities.checkType(parameter);
			log.info("param="+parameter.getName()+" Description="+parameter.getDescription()+ " xUnits="+xUnits + " yUnits="+yUnits + " dataType="+observationTypeForParam);
			
			
			List<ExperimentDTO> experimentList = observationService.getExperimentDTO(parameter.getId(), acc);
			System.out.println("Experiment dto marker="+experimentList);
			//ESLIM_003_001_003 id=962 calorimetry data for time series graph new MGI:1926153
			//http://localhost:8080/PhenotypeArchive/stats/genes/MGI:1926153?parameterId=ESLIM_003_001_003
			try{
			
			if(observationTypeForParam.equals(ObservationType.time_series)){
				//http://localhost:8080/PhenotypeArchive/stats/genes/MGI:1920000?parameterId=ESLIM_004_001_002
				if(parameter.isIncrementFlag()) {
					for(ParameterIncrement increment: parameter.getIncrement()) {
					System.out.println("increment="+increment);
					}
					
				}
				List<ChartData> timeSeriesForParam=timeSeriesChartAndTableProvider.doTimeSeriesData(bmDAO, experimentList, parameter, model, genderList, zyList , timeSeriesChartsAndTables.size()+1, biologicalModelsParams);
				timeSeriesChartsAndTables.addAll(timeSeriesForParam);
			}
			
			if(observationTypeForParam.equals(ObservationType.unidimensional)){
				//http://localhost:8080/phenotype-archive/stats/genes/MGI:1920000?parameterId=ESLIM_015_001_018
				log.info("calling chart creation for unidimensional data");
					UnidimensionalDataSet unidimensionalChartNTables = continousChartAndTableProvider.doUnidimensionalData(experimentList, bmDAO, config, unidimensionalMutantBiologicalModels, parameter, acc, model , genderList, zyList, ChartType.UnidimensionalBoxPlot);
				allUnidimensionalChartsAndTables.add(unidimensionalChartNTables);
			}
			if(observationTypeForParam.equals(ObservationType.categorical)){
				//https://dev.mousephenotype.org/mi/impc/dev/phenotype-archive/stats/genes/MGI:1346872?parameterId=ESLIM_001_001_004
			
					CategoricalResultAndCharts categoricalResultAndCharts=categoricalChartAndTableProvider.doCategoricalData(experimentList, bmDAO, config, parameter,  acc, model, genderList, zyList, biologicalModelsParams,
					charts, categoricalTables, parameterId);
					allCategoricalResultAndCharts.add(categoricalResultAndCharts);
			
			}
			} catch (SQLException e) {
				e.printStackTrace();
				statsError=true;
			}
		}// end of parameterId iterations

		model.addAttribute("unidimensionalMutantBiologicalModels", unidimensionalMutantBiologicalModels );
		model.addAttribute("allUnidimensionalChartsAndTables", allUnidimensionalChartsAndTables);
		model.addAttribute("timeSeriesMutantBiologicalModels", timeSeriesMutantBiologicalModels );
		model.addAttribute("timeSeriesChartsAndTables", timeSeriesChartsAndTables);
		model.addAttribute("categoricalMutantBModel", categoricalMutantBiologicalModels );
		model.addAttribute("allCategoricalResultAndCharts", allCategoricalResultAndCharts);
		model.addAttribute("statsError", statsError );
		return "stats";
	}

	private void generateProcedureView(String acc, Model model,
			GenomicFeature gene) {
		log.info("Gene Accession without any web params");
		//a method here to get a general page for Gene and all procedures associated
		List<PhenotypeCallSummary> allPhenotypeSummariesForGene = phenotypeCallSummaryDAO.getPhenotypeCallByAccession(acc);
		//a method here to get a general page for Gene and all procedures associated
		List<Pipeline> pipelines = pipelineDAO.getAllPhenotypePipelines();
		PipelineProcedureTablesCreator creator=new PipelineProcedureTablesCreator();
		List<PipelineProcedureData> dataForTables=creator.createArraysForTables(pipelines, allPhenotypeSummariesForGene, gene);
//			for(PipelineProcedureData dataForATable: dataForTables){
//				dataForATable.getTableData();
//			}
		model.addAttribute("pipelineProcedureData", dataForTables);
		//model.addAttribute("allPipelines", pipelines);//limit pipelines to two for testing
	}
	
	@RequestMapping("/stats/scatter/genes/{acc}")
	public String genesScatter(
			@RequestParam(required = false, /*defaultValue = "ESLIM_001_001_007",*/ value = "parameterId") String[] parameterIds,
			@RequestParam(required = false,  value = "gender") String[] gender,
			@RequestParam(required = false, value = "zygosity") String[] zygosity,
			@RequestParam(required = false, value = "model") String[] biologicalModelsParam,
			@PathVariable String acc, Model model)
			throws GenomicFeatureNotFoundException, IOException, URISyntaxException, SolrServerException {

		boolean statsError=false;
		// Get the global application configuration
		@SuppressWarnings("unchecked")
		Map<String, String> config = (Map<String, String>) bf
				.getBean("globalConfiguration");
		GenomicFeature gene = genesDao.getGenomicFeatureByAccession(acc);
		if (gene == null) {
			throw new GenomicFeatureNotFoundException("Gene " + acc
					+ " can't be found.", acc);
		}
		//
		log.info(gene.toString());
		model.addAttribute("gene", gene);

		List<String> paramIds = getParamsAsList(parameterIds);
		List<String> genderList = getParamsAsList(gender);
		List<String> zyList=getParamsAsList(zygosity);
		
		List<UnidimensionalDataSet> allUnidimensionalChartsAndTables=new ArrayList<UnidimensionalDataSet>();
		
		List<BiologicalModel> unidimensionalMutantBiologicalModels=new ArrayList<BiologicalModel>();
		
		
		for (String parameterId : paramIds) {
			ObservationType observationTypeForParam=null;
			 net.sf.json.JSONObject expResult=JSONGraphUtils.getExperimentalData(parameterId, acc, config);
			 JSONArray resultsArray=JSONRestUtil.getDocArray(expResult);
			 for(int i=0; i<resultsArray.size(); i++){
				 net.sf.json.JSONObject exp=resultsArray.getJSONObject(i);
				 observationTypeForParam = ObservationType.valueOf(exp.getString("observationType"));
			 }
			Parameter parameter = pipelineDAO.getParameterByStableIdAndVersion(parameterId, 1, 0);
			
			String[] parameterUnits=parameter.checkParameterUnits();
			String xUnits="";
			String yUnits="";
			
			if(parameterUnits.length>0){
			 xUnits=parameterUnits[0];
			}
			if(parameterUnits.length>1){
				yUnits=parameterUnits[1];
			}
			List<ExperimentDTO> experimentList = observationService.getExperimentDTO(parameter.getId(), acc);
			System.out.println("Experiment dto marker="+experimentList);
			log.info("param="+parameter.getName()+" Description="+parameter.getDescription()+ " xUnits="+xUnits + " yUnits="+yUnits + " dataType="+observationTypeForParam);
			
			//ESLIM_003_001_003 id=962 calorimetry data for time series graph new MGI:1926153
			//http://localhost:8080/PhenotypeArchive/stats/genes/MGI:1926153?parameterId=ESLIM_003_001_003
			try{
			
			if(observationTypeForParam.equals(ObservationType.unidimensional)){
				//http://localhost:8080/phenotype-archive/stats/genes/MGI:1920000?parameterId=ESLIM_015_001_018
				
				UnidimensionalDataSet unidimensionalChartNTables = continousChartAndTableProvider.doUnidimensionalData(experimentList, bmDAO, config, unidimensionalMutantBiologicalModels, parameter, acc, model , genderList, zyList, ChartType.UnidimensionalScatter);
				allUnidimensionalChartsAndTables.add(unidimensionalChartNTables);
			}
			
			} catch (SQLException e) {
				e.printStackTrace();
				statsError=true;
			}
		}// end of parameterId iterations

		model.addAttribute("unidimensionalMutantBiologicalModels", unidimensionalMutantBiologicalModels );
		model.addAttribute("allUnidimensionalChartsAndTables", allUnidimensionalChartsAndTables);
		model.addAttribute("statsError", statsError );
		return "scatter";
	}
	
	
	/**
	 * For Testing not for users-  view the parameters and genes as links to stats pages for timeseries data
	 * @param start default 0 if not specified
	 * @param length default 100 if not specified
	 * @param model
	 * @return
	 * @throws GenomicFeatureNotFoundException
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws SQLException 
	 */
	@RequestMapping("/stats/statslinks")
	public String statsLinksView(
			@RequestParam(required = false, value = "start") Integer start,
			@RequestParam(required = false, value = "length") Integer length,
			@RequestParam(required = false, value = "observationType") String observationType,
			 @RequestParam(required = false, /*defaultValue = "ESLIM_001_001_007",*/ value = "parameterId") String[] parameterIds,
			 Model model)
			throws GenomicFeatureNotFoundException, IOException, URISyntaxException, SQLException {
		System.out.println("calling stats links");
		//equivalent url from solr service http://wwwdev.ebi.ac.uk/mi/impc/dev/solr/experiment/select?q=observationType:unidimensional&wt=json&indent=true&start=0&rows=10
		ObservationType oType=null;
		for(ObservationType type: ObservationType.values()){
			System.out.println(type.name());
			if(type.name().equalsIgnoreCase(observationType)){
				oType=type;
			}
		}
		System.out.println("calling observation type="+oType);
		List<String> paramIds = getParamsAsList(parameterIds);
	
			getLinksForStats(start, length, model, oType, paramIds);
		
		
		return "statsLinksList";
	}

	/**
	 * for testing - not for users
	 * @param start
	 * @param length
	 * @param model
	 * @param type
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws SQLException 
	 */
	private void getLinksForStats(Integer start, Integer length, Model model, ObservationType type, List<String>parameterIds) throws IOException, URISyntaxException, SQLException {
		if(start==null)start=0;
		if(length==null)length=100;
		@SuppressWarnings("unchecked")
//		Map<String, String> config = (Map<String, String>) bf
//				.getBean("globalConfiguration");
//		String url=config.get("internalSolrUrl")+"/experiment/select?q=observationType:"+type+"&wt=json&indent=true&start="+start+"&rows="+length;
//		net.sf.json.JSONObject result = JSONRestUtil.getResults(url);
//		System.out.println(result.toString());
//		JSONArray resultsArray=JSONRestUtil.getDocArray(result);
//	
//			System.out.println(start+" end="+length);
//			 List<Map<String, String>> listWithStableId=new ArrayList<Map<String, String>>();
//			 for(int i=0; i<resultsArray.size(); i++){
//				 Map<String,String> map=new HashMap<String,String>();
//				net.sf.json.JSONObject exp=resultsArray.getJSONObject(i);
//				String statbleParamId=exp.getString("parameterStableId");
//				int internalParamId=exp.getInt("parameterId");
//				String accession=exp.getString("geneAccession");
//				 System.out.println(accession+" parameter="+statbleParamId);
//				// Parameter parameter=pipelineDAO.getParameterById(Integer.valueOf(parameterId));
//				 map.put("paramStableId", statbleParamId);
//				 map.put("accession",accession);
//				 listWithStableId.add(map);
//			 }
			 
				List<Map<String, String>> list=null;
				if(type==ObservationType.time_series){
				  list = timeSeriesStatisticsDAO.getListOfUniqueParametersAndGenes(start, length);
				}
				if(type==ObservationType.unidimensional){
					if(parameterIds.size()>0){
						for(String paramId: parameterIds){
							List<Map<String, String>>tempList=unidimensionalStatisticsDAO.getListOfUniqueParametersAndGenes(start, length, paramId);
							list.addAll(tempList);
						}
					}
					  list = unidimensionalStatisticsDAO.getListOfUniqueParametersAndGenes(start, length);
					}
				if(type==ObservationType.categorical){
					  list = categoricalStatsDao.getListOfUniqueParametersAndGenes(start, length);
					}
				
				 List<Map<String, String>> listWithStableId=new ArrayList<Map<String, String>>();
				 for(Map<String, String> row :list){
					 Map<String,String> map=new HashMap<String,String>();
					String parameterId=row.get("parameter_id");
					String accession=row.get("accession");
					 System.out.println(accession+" parameter="+parameterId);
					 Parameter parameter=pipelineDAO.getParameterById(Integer.valueOf(parameterId));
					 map.put("paramStableId",parameter.getStableId());
					 map.put("accession",accession);
					 listWithStableId.add(map);
				 }

			 
			 model.addAttribute("statsLinks", listWithStableId);
		
	}

	/**
	 * Convenience method that just changes an array [] to a more modern LIst (I hate arrays! :) )
	 * @param parameterIds
	 * @return
	 */
	private List<String> getParamsAsList(String[] parameterIds) {
		List<String> paramIds = null;
		if (parameterIds == null) {
			 paramIds=Collections.emptyList();
		}else{
			paramIds = Arrays.asList(parameterIds); 
		}
		return paramIds;
	}
	
	



	/**
	 * required for implementing BeanFactoryAware
	 * 
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	@Override
	public void setBeanFactory(BeanFactory arg0) throws BeansException {
		this.bf = arg0;
	}

	
	
	@ExceptionHandler(GenomicFeatureNotFoundException.class)
	public ModelAndView handleGenomicFeatureNotFoundException(GenomicFeatureNotFoundException exception) {
        ModelAndView mv = new ModelAndView("identifierError");
        mv.addObject("errorMessage",exception.getMessage());
        mv.addObject("acc",exception.getAcc());
        mv.addObject("type","MGI gene");
        mv.addObject("exampleURI", "/stats/genes/MGI:104874");
        return mv;
    } 

}
