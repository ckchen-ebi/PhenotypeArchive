<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:genericpage>
	<jsp:attribute name="title">Stats for ${gene.name}</jsp:attribute>

	<jsp:attribute name="breadcrumb">&nbsp;&raquo; <a
			href="${baseUrl}/search#sort=marker_symbol asc&q=*:*&core=gene">Genes</a> <c:if
			test="${not empty gene.subtype.name }">&raquo; <a
				href='${baseUrl}/search#fq=marker_type:"${gene.subtype.name}"&q=*:*&core=gene'>${gene.subtype.name}</a>
		</c:if> &raquo; ${gene.symbol}</jsp:attribute>

	<jsp:attribute name="header">

	<script type="text/javascript">
		var gene_id = '${acc}';
	</script>

	<!--    extra header stuff goes here such as extra page specific javascript -->
	<!-- highcharts -->
<script type='text/javascript' src='${baseUrl}/js/charts/highcharts.js'></script>
 <script type='text/javascript' src='${baseUrl}/js/charts/highcharts-more.js'></script> 
<script src="${baseUrl}/js/charts/exporting.js"></script>
	

	

<style>
</style>

    </jsp:attribute>

	<jsp:body>
        
		<div class='topic'>Gene: ${gene.symbol}</div>
		<c:if test="${statsError}">
					<div class="alert alert-error">
							  				<strong>Error:</strong> A stats error occured - results on this page maybe incorrect.
					</div>
		</c:if>
	

<!-- time series charts here-->

<c:forEach var="timeChart" items="${timeSeriesChartsAndTables}" varStatus="timeLoop">
 <%-- ${loop.count  % 2} --%>
	<c:if test = "${timeLoop.count  % 2!=0}">
		<div class="row-fluid dataset">  
		 <div class="row-fluid ">
		 		<div class="container span6"><h4><!--  style="background-color:lightgrey;"  -->Allele -  <t:formatAllele>${timeSeriesMutantBiologicalModels[timeLoop.index].alleles[0].symbol}</t:formatAllele> <span class="graphGenBackground">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;background -  ${timeSeriesMutantBiologicalModels[timeLoop.index].geneticBackground}</span></h4>
		 		</div>
		 		 <c:if test="${fn:length(timeSeriesChartsAndTables) > (timeLoop.index+1)}">
		 		 <div class="container span6"><h4><!--  style="background-color:lightgrey;"  -->Allele -  <t:formatAllele>${timeSeriesMutantBiologicalModels[timeLoop.index+1].alleles[0].symbol}</t:formatAllele> <span class="graphGenBackground">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;background -  ${timeSeriesMutantBiologicalModels[timeLoop.index+1].geneticBackground}</span></h4>
		 		</div>
		 		</c:if>
 		</div>
 		<div class="row-fluid">
 	</c:if>
  				 <div class="container span6">
								<div id="timeChart${timeLoop.count}"
									style="min-width: 400px; height: 450px; margin: 0 auto">
								</div>
								<script type="text/javascript">
								${timeChart.chart}
								</script>
					</div>
<c:if test = "${(timeLoop.count % 2==0) || timeLoop.count eq fn:length(timeSeriesChartsAndTables)}">
		 </div>
		 </div>
</c:if>
</c:forEach>




<c:forEach var="categoricalResultAndCharts" items="${allCategoricalResultAndCharts}" varStatus="maleAndFemaleLoop">
	<div class="row-fluid dataset">
	<c:if test="${fn:length(categoricalResultAndCharts.maleAndFemale)==0}">
	No data for this zygosity and gender for this parameter and gene
	</c:if>
	<c:if test="${fn:length(categoricalResultAndCharts.maleAndFemale)>0}">
 		<div class="row-fluid">
				<div class="container span12">
		 				<h4>Allele - <t:formatAllele>${categoricalResultAndCharts.maleAndFemale[0].biologicalModel.alleles[0].symbol}</t:formatAllele> <span class="graphGenBackground"> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;background -  ${categoricalResultAndCharts.maleAndFemale[0].biologicalModel.geneticBackground} </span></h4>
		 		</div>
		 </div>
		
				<div class="row-fluid">
 				<c:forEach var="categoricalChartDataObject" items="${categoricalResultAndCharts.maleAndFemale}" varStatus="chartLoop">
  				 	<div class="container span6">
								<div id="categoricalBarChart${categoricalChartDataObject.chartIdentifier}"
									style="min-width: 400px; height: 400px; margin: 0 auto">
								</div>
   								<script type="text/javascript">
   								${categoricalChartDataObject.chart}
   							</script>
					</div>
 				</c:forEach>
				</div>
 <%--  ${categoricalResultAndCharts.maleAndFemale} --%>
 				<div class="row-fluid">
						<table id="catTable${categoricalChartDataObject.chartIdentifier}" class="table table-bordered  table-striped table-condensed">
										<thead><tr>
										<th>Control Gender</th><th>Exp Gender</th><th>ctrl zygosity</th><th>exp zygosity</th><th>Ctrl ${categoricalResultAndCharts.statsResults[0].categoryA}</th><th>Ctrl ${categoricalResultAndCharts.statsResults[0].categoryB}</th><th>Exp ${categoricalResultAndCharts.statsResults[0].categoryA}</th><th>Exp ${categoricalResultAndCharts.statsResults[0].categoryB}</th><th>p Value</th><th>Effect size</th>
										</tr></thead>
										<tbody>
										<c:forEach var="catStatsResult"  items="${categoricalResultAndCharts.statsResults}" varStatus="catStatsStatus">
										<tr>
											<td> ${catStatsResult.controlSex }</td><td> ${catStatsResult.experimentalSex }</td><td>${catStatsResult.controlZygosity }</td><td>${catStatsResult.experimentalZygosity }</td>
											<td>
													<c:forEach var="sexCatObject" items="${categoricalResultAndCharts.maleAndFemale}">
												
														<c:if test="${sexCatObject.sexType==catStatsResult.experimentalSex}" >
  
														<%-- blah ${sexCatObject.sexType} ${catStatsResult.experimentalSex} --%>
																<c:forEach var="controlHomOrHetObjectCatSetObject" items="${sexCatObject.categoricalSets}">
																	<c:forEach var="catObject" items="${controlHomOrHetObjectCatSetObject.catObjects }">
																	<c:if test="${catObject.name =='control' && catObject.category==catStatsResult.categoryA}" >${catObject.count }
																	</c:if>
																	</c:forEach>
																</c:forEach>
														</c:if> 
													</c:forEach>
											</td>
											<td><c:forEach var="sexCatObject" items="${categoricalResultAndCharts.maleAndFemale}">
												
														<c:if test="${sexCatObject.sexType==catStatsResult.experimentalSex}" >
  
														<%-- blah ${sexCatObject.sexType} ${catStatsResult.experimentalSex} --%>
																<c:forEach var="controlHomOrHetObjectCatSetObject" items="${sexCatObject.categoricalSets}">
																	<c:forEach var="catObject" items="${controlHomOrHetObjectCatSetObject.catObjects }">
																	<c:if test="${catObject.name =='control' && catObject.category==catStatsResult.categoryB}" >${catObject.count }
																	</c:if>
																	</c:forEach>
																</c:forEach>
														</c:if> 
													</c:forEach>
											</td>
											<td>
												<c:forEach var="sexCatObject" items="${categoricalResultAndCharts.maleAndFemale}">
												
														<c:if test="${sexCatObject.sexType==catStatsResult.experimentalSex}" >
  
														<%--  ${sexCatObject.sexType} ${catStatsResult.experimentalSex} --%>
																<c:forEach var="controlHomOrHetObjectCatSetObject" items="${sexCatObject.categoricalSets}">
																	<c:forEach var="catObject" items="${controlHomOrHetObjectCatSetObject.catObjects }">
																	<%-- catName=${catObject.name} zyg= ${catStatsResult.experimentalZygosity } --%>
																	<c:if test="${(catObject.name ==catStatsResult.experimentalZygosity.name) && catObject.category==catStatsResult.categoryA}" > ${catObject.count }
																	</c:if>
																	</c:forEach>
																</c:forEach>
														</c:if> 
													</c:forEach>
												
											</td>
											<td>
											<c:forEach var="sexCatObject" items="${categoricalResultAndCharts.maleAndFemale}">
												
														<c:if test="${sexCatObject.sexType==catStatsResult.experimentalSex}" >
  
														<%--  ${sexCatObject.sexType} ${catStatsResult.experimentalSex} --%>
																<c:forEach var="controlHomOrHetObjectCatSetObject" items="${sexCatObject.categoricalSets}">
																	<c:forEach var="catObject" items="${controlHomOrHetObjectCatSetObject.catObjects }">
																	<%-- catName=${catObject.name} zyg= ${catStatsResult.experimentalZygosity } --%>
																	<c:if test="${(catObject.name ==catStatsResult.experimentalZygosity.name) && catObject.category==catStatsResult.categoryB}" > ${catObject.count }
																	</c:if>
																	</c:forEach>
																</c:forEach>
														</c:if> 
											</c:forEach>
											</td>
											<td>${catStatsResult.pValue }</td><td>${catStatsResult.maxEffect}</td>
										</tr>
										</c:forEach>
						</table>
					<%-- 	<c:forEach var="sexCatObject" items="${categoricalResultAndCharts.maleAndFemale}">
						${sexCatObject.sexType}<br/>
								<c:forEach var="controlHomOrHetObjectCatSetObject" items="${sexCatObject.categoricalSets}">
									${controlHomOrHetObjectCatSetObject.catObjects } <br/>
								</c:forEach>
						</c:forEach> --%>
				</div>
				</c:if>
 	</div>
 </c:forEach>

			
				
			
	
		
		<!--/end of categoriacl charts-->
		
 <c:forEach var="unidimensionalData" items="${allUnidimensionalChartsAndTables}" varStatus="unidimensionalDataLoop">
 <div class="row-fluid dataset"> 
 <c:if test="${fn:length(unidimensionalData.statsObjects)==0}">
		No data for this zygosity and gender for this parameter and gene
		</c:if>
		<c:if test="${fn:length(unidimensionalData.statsObjects)>0}">
		 <div class="row-fluid">
		 		<div class="container span12"><h4>Allele -  <t:formatAllele> ${unidimensionalData.statsObjects[1].allele }</t:formatAllele> <span class="graphGenBackground">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;background -  ${unidimensionalData.statsObjects[1].geneticBackground }</span></h4>
				 </div>
 		</div>
 		<div class="row-fluid">
<c:forEach var="unidimensionalChartsAndTable" items="${unidimensionalData.sexChartAndTables}" varStatus="uniDimensionalLoop">
 <%-- ${loop.count  % 2} --%>

  		<div class="container span6">
				<div id="chart${uniDimensionalLoop.count}"
									style="min-width: 400px; height: 400px; margin: 0 auto">
				</div>
		
		
								
								
			
   								<script type="text/javascript">
   
   								$(function () {
   								    $('#chart${uniDimensionalLoop.count}').highcharts(${unidimensionalChartsAndTable.chart});
								</script>
								
		</div><!-- end of span6  individual chart holder -->
		</c:forEach>
		<table id="continuousTable${uniDimensionalLoop.count}" class="table table-bordered  table-striped table-condensed">
		<thead><tr>
		<th>Line</th>
		<th>Zygosity</th>
			<th>Sex</th>
			<th>Mean</th>
			<th>SD</th>
			<th>Sample Size</th>
		<%-- <th>${tables[loop.count-1].xAxisCategories[1]}</th><th>${tables[loop.count-1].xAxisCategories[2]}</th> --%>
		</tr></thead>
		<tbody>
										
										
										
											<c:forEach var="statsObject" items="${unidimensionalData.statsObjects}">
												<tr>
												<td>${statsObject.line}</td>
												<c:choose>
												<c:when test="${statsObject.line =='Control' }">
												<td>NA</td>
												</c:when>
												<c:when test="${statsObject.line !='Control' }">
												<td>${statsObject.zygosity}</td>
												</c:when>
												</c:choose>
												<td>${statsObject.sexType}</td>
												<td>${statsObject.mean}</td>
												<td>${statsObject.sd}</td>
												<td>${statsObject.sampleSize}</td>
												</tr>
												</c:forEach>
												
									
										
										<%-- <td>${tables[loop.count-1].seriesDataForCategoricalType[0][0]}</td><td>${tables[loop.count-1].seriesDataForCategoricalType[0][1]}</td><td>${tables[loop.count-1].seriesDataForCategoricalType[0][2]}</td>
										 </tr>--%>
										<%-- <tr><td>${tables[loop.count-1].categories[1]}</td><td>${tables[loop.count-1].seriesDataForCategoricalType[1][0]}</td><td>${tables[loop.count-1].seriesDataForCategoricalType[1][1]}</td><td>${tables[loop.count-1].seriesDataForCategoricalType[1][2]}</td>
										 </tr>--%>
										</tbody>
										</table>
				
				
				<c:if test="${fn:length(unidimensionalData.allUnidimensionalResults)>0}">
				<div class="row-fluid">
						<div class="container span12">
						<table class="ttable table-bordered  table-striped table-condensed">
						<%-- ${fn:length(unidimensionalData.allUnidimensionalResults)} --%>
						
 							<c:forEach var="data" items="${unidimensionalData.allUnidimensionalResults}">
 							<%-- <td>${data.significanceClassification}</td> --%>
 									<c:choose>
          									<c:when test="${data.significanceClassification == 'both_equally' || data.significanceClassification == 'none'  || data.significanceClassification == 'cannot_classify' }">
          												<tr><th>Global Test</th><th>Significance/Classification</th><th>Effect</th></tr>
          												<tr><td>${data.nullTestSignificance}</td><td>${data.significanceClassification.text}</td><td>${data.genotypeParameterEstimate}</td></tr></c:when>
         									<c:when test="${data.significanceClassification == 'female_only' || data.significanceClassification == 'male_only'  || data.significanceClassification == 'female_greater' || data.significanceClassification == 'male_greater' || data.significanceClassification == 'different_directions'}">
       													 <tr><th>Global Test</th><th>Significance/Classification</th><th>Gender</th><th>Effect</th></tr>
       													 <tr>
       													 <td rowspan="2">${data.nullTestSignificance}</td>
       													 <td rowspan="2">${data.significanceClassification.text}</td>
       													 <td>Female</td><td>${data.genderFemaleKoEstimate}</td>
       													 <tr><td> Male</td><td> ${data.genderMaleKoEstimate}</td></tr></c:when>
									</c:choose>
									</c:forEach>
 	 							</table>
 	 							
 	 		<%-- <table class="ttable table-bordered  table-striped table-condensed">
						<tr>
						<th>colonyId</th>
						<th>experimentalZygosity</th>
						<th>mixedModel</th>
						<th>dependantVariable</th>
						<th>batchSignificance</th>
						<th>varianceSignificance</th>
						<th>nullTestSignificance</th>
						<th>genotypeParameterEstimate</th>
						<th>genotypeStandardErrorEstimate</th>
						<th>genotypeEffectPValue</th>
						<th>genderParameterEstimate</th>
						<th>genderStandardErrorEstimate</th>
						<th>genderEffectPValue</th>
						<th>weightParameterEstimate</th>
						<th>weightStandardErrorEstimate</th>
						<th>weightEffectPValue</th>
						<th>gp1Genotype</th>
						<th>gp1ResidualsNormalityTest</th>
						<th>gp2Genotype</th>
						<th>gp2ResidualsNormalityTest</th>
						<th>blupsTest</th>
						<th>rotatedResidualsNormalityTest</th>
						<th>interceptEstimate</th>
						<th>interceptEstimateStandardError</th>
						<th>interactionSignificance</th>
						<th>interactionEffectPValue</th>
						<th>genderFemaleKoEstimate</th>
						<th>genderFemaleKoStandardErrorEstimate</th>
						<th>genderFemaleKoPValue</th>
						<th>genderMaleKoEstimate</th>
						<th>genderMaleKoStandardErrorEstimate</th>
						<th>genderMaleKoPValue</th>
						</tr>
 						<c:forEach var="data" items="${unidimensionalData.allUnidimensionalResults}">
 							<tr>
 							<td>${data.colonyId }</td>
 							<td>${data.experimentalZygosity}</td>
 							<td>${data.mixedModel}</td>
 							<td>${data.dependantVariable}</td>
 							<td>${data.batchSignificance }</td>
 							<td>${data.varianceSignificance }</td>
 							<td>${data.nullTestSignificance }</td>
 							<td>${data.genotypeParameterEstimate }</td>
 							<td>${data.genotypeStandardErrorEstimate }</td>
 							<td>${data.genotypeEffectPValue}</td>
 							<td>${data.genderParameterEstimate }</td>
 							<td>${data.genderStandardErrorEstimate }</td>
 							<td>${data.genderEffectPValue}</td>
 							<td>${data.weightParameterEstimate }</td>
 							<td>${data.weightStandardErrorEstimate }</td>
 							<td>${data.weightEffectPValue }</td>
 							<td>${data.gp1Genotype }</td>
 							<td>${data.gp1ResidualsNormalityTest }</td>
 							<td>${data.gp2Genotype }</td>
 							<td>${data.gp2ResidualsNormalityTest }</td>
 							<td>${data.blupsTest }</td>
 							<td>${data.rotatedResidualsNormalityTest }</td>
 							<td>${data.interceptEstimate }</td>
 							<td>${data.interceptEstimateStandardError }</td>
 							<td>${data.interactionSignificance }</td>
 							<td>${data.interactionEffectPValue }</td>
 							<td>${data.genderFemaleKoEstimate }</td>
 							<td>${data.genderFemaleKoStandardErrorEstimate }</td>
 							<td>${data.genderFemaleKoPValue }</td>
 							<td>${data.genderMaleKoEstimate }</td>
 							<td>${data.genderMaleKoStandardErrorEstimate }</td>
 							<td>${data.genderMaleKoPValue }</td>
 							</tr>
						</c:forEach>
 	 					</table>--%>
 	 							
 						</div>
 				</div>
 				</c:if>	
 				</c:if>
	<!-- </div> -->
</div><!-- end of row fluid data set -->
</c:forEach><!--  end of undimensional data loop -->




    </jsp:body>
</t:genericpage>
