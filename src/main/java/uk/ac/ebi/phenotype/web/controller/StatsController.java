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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.ebi.phenotype.dao.BiologicalModelDAO;
import uk.ac.ebi.phenotype.dao.CategoricalStatisticsDAO;
import uk.ac.ebi.phenotype.dao.GenomicFeatureDAO;
import uk.ac.ebi.phenotype.dao.PhenotypeCallSummaryDAO;
import uk.ac.ebi.phenotype.dao.PhenotypePipelineDAO;
import uk.ac.ebi.phenotype.error.GenomicFeatureNotFoundException;
import uk.ac.ebi.phenotype.pojo.BiologicalModel;
import uk.ac.ebi.phenotype.pojo.CategoricalTableObject;
import uk.ac.ebi.phenotype.pojo.GenomicFeature;
import uk.ac.ebi.phenotype.pojo.Parameter;
import uk.ac.ebi.phenotype.pojo.Pipeline;
import uk.ac.ebi.phenotype.pojo.SexType;
import uk.ac.ebi.phenotype.pojo.ZygosityType;

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
	private PhenotypeCallSummaryDAO phenoDAO;

	/**
	 * Runs when the request missing an accession ID. This redirects to the
	 * search page which defaults to showing all genes in the list
	 */
	@RequestMapping("/stats")
	public String rootForward() {
		return "redirect:/search";
	}

	@RequestMapping("/stats/genes/{acc}")
	public String genesStats(
			@RequestParam(required = false, /*defaultValue = "ESLIM_001_001_007",*/ value = "parameterId") String[] parameterIds,
			@RequestParam(required = false, value = "gender") String[] gender,
			@RequestParam(required = false, value = "zygosity") String[] zygosity,
			@RequestParam(required = false, value = "model") String[] biologicalModelsParam,
			@PathVariable String acc, Model model, HttpServletRequest request,
			RedirectAttributes attributes)
			throws GenomicFeatureNotFoundException {

		// Get the global application configuration
		@SuppressWarnings("unchecked")
		Map<String, String> config = (Map<String, String>) bf
				.getBean("globalConfiguration");

		GenomicFeature gene = genesDao.getGenomicFeatureByAccession(acc);
		if (gene == null) {
			throw new GenomicFeatureNotFoundException("Gene " + acc
					+ " can't be found.", acc);
		}
		model.addAttribute("gene", gene);

		List<String> paramIds = getParamsAsList(parameterIds);
		List<String> genderList = getParamsAsList(gender);
		List<String> zyList=getParamsAsList(zygosity);
		List<String> biologicalModelsParams=getParamsAsList(biologicalModelsParam);
		
		if(paramIds.isEmpty() && genderList.isEmpty() && zyList.isEmpty() && biologicalModelsParams.isEmpty()){
			System.out.println("Gene Accession without any web params");
			//a method here to get a general page for Gene and all procedures associated
			List<Pipeline> pipelines = pipelineDAO.getAllPhenotypePipelines();
			model.addAttribute("allPipelines", pipelines);
			return "procedureStats";
		}

		// MGI:105313 male het param 655
		// System.out.println("acc=" + acc);
		List<JSONObject> charts = new ArrayList<JSONObject>();
		List<CategoricalTableObject> tables=new ArrayList<CategoricalTableObject>();
		// param 655
		// female homzygote
		// population id=4640 or 4047 - male, het.
		int chartNumber = 1;

		// if one or more parameterIds specified in the url do this
		for (String parameterId : paramIds) {

			Parameter parameter = pipelineDAO.getParameterByStableIdAndVersion(parameterId, 1, 0);
			List<String> categories = categoricalStatsDao.getCategories(parameter);
			List<BiologicalModel> biologicalModels = categoricalStatsDao.getBiologicalModelsByParameterAndGene(parameter, acc);

			model.addAttribute("parameterId", parameter.getId().toString());
			model.addAttribute("parameterDescription", parameter.getDescription());

			System.out.println("biological models list size=" + biologicalModels.size());

			for (BiologicalModel biologicalModel : biologicalModels) {
				if(biologicalModelsParams.isEmpty()||biologicalModelsParams.contains(biologicalModel.getId().toString())){

					System.out.println("biologicalModel="+biologicalModel);
					List<Integer> popIds = categoricalStatsDao.getPopulationIdsByParameterAndMutantBiologicalModel(parameter, biologicalModel);

					System.out.println("Population IDs: "+popIds);
					for(Integer popId:popIds){

						SexType sexType = categoricalStatsDao.getSexByPopulation(new Integer(popId.intValue()));//(new Integer(5959));
						//System.out.println(popId+" sextype="+sexType);

						if(genderList.isEmpty()||genderList.contains(sexType.name())){
							List<String> xAxisCategories = this.getXAxisCategories(zyList);
							List<List<Long>> seriesDataForCategoricalType = new ArrayList<List<Long>>();

							// category e.g normal, abnormal
							for (String category : categories) {
								List<Long> categoryData = new ArrayList<Long>();
								Long controlCount = categoricalStatsDao.countControl(sexType, parameter, category, popId);
								categoryData.add(controlCount);

								// System.out.println("control=" + sexType.name() + " count=" + controlCount + " category=" + category);
								for (ZygosityType zType : ZygosityType.values()) {
									if (zType.equals(ZygosityType.hemizygote)) {
										continue;
									}

									//System.out.println("control="+zType.name() + " count=" + controlCount +" category="+category);
									//System.out.println(zyList);
									if(zyList.isEmpty()||zyList.contains(zType.name())){
										Long mutantCount = categoricalStatsDao.countMutant(sexType, zType, parameter, category, popId);
										categoryData.add(mutantCount);
									}
								}
								seriesDataForCategoricalType.add(categoryData);
							}
							
							Set<Integer> removeColumns = new HashSet<Integer>();
							for (int i=1;i<seriesDataForCategoricalType.get(0).size();i++) {
								int count = 0;
								for (int j=0;j<seriesDataForCategoricalType.size();j++) {
									count+=seriesDataForCategoricalType.get(j).get(i);
								}
								if (count == 0) {
									//remove the column as there is no data
									removeColumns.add(i);
								}
							}
							if(!removeColumns.isEmpty()) {
								for (Integer column:removeColumns) {
									for (List<Long> ll:seriesDataForCategoricalType) {
										ll.remove(column.intValue());
									}
									xAxisCategories.remove(column.intValue());
								}
							}

							JSONObject chart = this.createHighChart(sexType,
								parameter.getDescription() + " " + sexType,
								xAxisCategories, categories,
								seriesDataForCategoricalType, "chart" + chartNumber);
							CategoricalTableObject table = this.creatCategoricalDataTable(model, sexType,
								parameter.getDescription() + " " + sexType,
								xAxisCategories, categories,
								seriesDataForCategoricalType, "table" + chartNumber);
							tables.add(table);
							charts.add(chart);
							chartNumber++;
						}//end of gender
					}
				}//end of biological model param
			}
		}// end of parameterId iterations

		// ArrayList<String> xAxisCategories=new ArrayList<String>();
		// xAxisCategories.add("Control");
		// xAxisCategories.add("Heterozygote");
		// xAxisCategories.add("Homozygote");
		// List<String> categoricalDataTypesTitles=new ArrayList<String>();
		// categoricalDataTypesTitles.add("Abnormal Femur");
		// categoricalDataTypesTitles.add("Normal");
		// List<List<Long>>seriesDataForCategoricalType=new
		// ArrayList<List<Long>>();
		// List<Long>abnormalFemur=new ArrayList<Long>();
		// abnormalFemur.add(new Long(2));
		// abnormalFemur.add(new Long(2));
		// abnormalFemur.add(new Long(3));
		// List<Long>normalFemur=new ArrayList<Long>();
		// normalFemur.add(new Long(5));
		// normalFemur.add(new Long(3));
		// normalFemur.add(new Long(4));
		// seriesDataForCategoricalType.add(abnormalFemur);
		// seriesDataForCategoricalType.add(normalFemur);
		// this.createHighChart(model, SexType.female, "New Title Here",
		// xAxisCategories, categoricalDataTypesTitles,
		// seriesDataForCategoricalType);
		// this.createHighChart(model, SexType.male, "New Title Here",
		// xAxisCategories, categoricalDataTypesTitles,
		// seriesDataForCategoricalType);
		model.addAttribute("charts", charts);
		model.addAttribute("tables", tables);
		//System.out.println("model="+model);
		return "stats";
	}

	private List<String> getParamsAsList(String[] parameterIds) {
		List<String> paramIds = null;
		if (parameterIds == null) {
			 paramIds=Collections.EMPTY_LIST;
		}else{
			paramIds = Arrays.asList(parameterIds); 
		}
		return paramIds;
	}

	private CategoricalTableObject creatCategoricalDataTable(Model model, SexType sexType, String title,
			List<String> xAxisCategories, List<String> categories,
			List<List<Long>> seriesDataForCategoricalType, String renderTo) {
		CategoricalTableObject tableObject=new CategoricalTableObject();
		tableObject.setSexType(sexType.name());
		tableObject.setTitle(title);
		tableObject.setxAxisCategories(xAxisCategories);
		tableObject.setCategories(categories);
		
		tableObject.setSeriesDataForCategoricalType(seriesDataForCategoricalType);
		return tableObject;
		
	}

	private List<String> getXAxisCategories(List<String> zygocityParams) {
		List<String> xAxisCat = new ArrayList<String>();
		xAxisCat.add("Control");// we know we have controls and we want to put
								// these first.
		
		for (ZygosityType type : ZygosityType.values()) {
			if(zygocityParams.isEmpty()||zygocityParams.contains(type.name())){
			if (type.equals(ZygosityType.hemizygote))
				continue;
			xAxisCat.add(type.name());
			}
		}
		return xAxisCat;
	}

	/**
	 * 
	 * @param model
	 *            mvc model from spring
	 * @param sex
	 *            SexType
	 * @param title
	 *            main title for the graph
	 * @param xAxisCategories
	 *            e.g. Control, Homozygote, Heterozygote
	 * @param categoricalDataTypesTitles
	 *            e.g. Abnormal, Normal
	 * @param seriesDataForCategoricalType
	 *            e.g.
	 * @return
	 */
	private JSONObject createHighChart(SexType sex, String title,
			List<String> xAxisCategories,
			List<String> categoricalDataTypesTitles,
			List<List<Long>> seriesDataForCategoricalType, String renderTo) {
		JSONObject highChartsGraph = null;
		try {
			highChartsGraph = new JSONObject(
					"{ chart: { renderTo: 'female', type: 'column' }, title: { text: '' }, xAxis: { categories: [] }, yAxis: { min: 0, title: { text: '' } },  plotOptions: { column: { stacking: 'percent' } }, series: [ ] }");

			System.out.println("call to highchart" + " sex=" + sex + " title="
					+ title + " xAxisCategories=" + xAxisCategories
					+ "  categoricalDataTypesTitles="
					+ categoricalDataTypesTitles
					+ " seriesDataForCategoricalType="
					+ seriesDataForCategoricalType);
			// "{ chart: { renderTo: 'female', type: 'column' }, title: { text: '' }, xAxis: { categories: [] }, yAxis: { min: 0, title: { text: '' } },  plotOptions: { column: { stacking: 'percent' } }, series: [ { name: 'Abnormal Femur', color: '#AA4643', data: [2, 2, 3] },{ name: 'Normal', color: '#4572A7', data: [5, 3, 4] }] }"
			String colorNormal = "#4572A7";
			String colorAbnormal = "#AA4643";
			String color = "";
			JSONObject chart = new JSONObject();
			chart.put("renderTo", renderTo);
			chart.put("type", "column");
			highChartsGraph.put("chart", chart);
			
			//tooltip function placeholder to be overriden in the jsp page as can't add method here!!
			highChartsGraph.put("tooltip", "{tooltipPlaceHolder}");
			 //tooltip: {blah:{}

			JSONObject titleJ = new JSONObject();
			titleJ.put("text", title);
			highChartsGraph.put("title", titleJ);
			JSONObject xAxisCategoriesObj = new JSONObject();
			JSONArray xAxisCategoriesArray = new JSONArray();
			for (String xAxisCategory : xAxisCategories) {
				xAxisCategoriesArray.put(xAxisCategory);
			}

			xAxisCategoriesObj.put("categories", xAxisCategoriesArray);
			highChartsGraph.put("xAxis", xAxisCategoriesObj);
			JSONArray seriesArray = new JSONArray();
			int i = 0;
			for (List<Long> data : seriesDataForCategoricalType) {
				JSONObject dataset1 = new JSONObject();// e.g. normal
				dataset1.put("name", categoricalDataTypesTitles.get(i));
				if (i == 0) {
					color = colorNormal;
				} else {
					color = colorAbnormal;
				}
				dataset1.put("color", color);
				JSONArray dataset = new JSONArray();

				for (Long singleValue : data) {
					dataset.put(singleValue);
				}
				dataset1.put("data", dataset);
				seriesArray.put(dataset1);
				i++;
			}

			highChartsGraph.put("series", seriesArray);
			// String
			// tooltipMethod="function() {         return ' '+       this.series.name +': '+ this.y +' ('+ Math.round(this.percentage) +'%)';    ";
			// JSONObject methodObject=new JSONObject(tooltipMethod);
			// methodObject.put("formatter", tooltipMethod);
			// female.put("tooltip", methodObject );
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("model="+model);
		return highChartsGraph;
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

	@RequestMapping("/statsOld/genes/{acc}")
	public String genesStats2(Model model, HttpServletRequest request,
			RedirectAttributes attributes) {
		return "statsOld";
	}
}
