package uk.ac.ebi.phenotype.pojo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import uk.ac.ebi.generic.util.JSONRestUtil;
import uk.ac.ebi.phenotype.util.PhenotypeCallSummaryDAOReadOnly;
import uk.ac.ebi.phenotype.util.PhenotypeFacetResult;

public class PhenotypeCallSummarySolrImpl implements
		PhenotypeCallSummaryDAOReadOnly {
	
	@Resource(name="globalConfiguration")
	private Map<String, String> config;
//TODO change this to come from the configuration
	private final String core="genotype-phenotype";

	@Override
	public PhenotypeFacetResult getPhenotypeCallByGeneAccession(String accId) throws IOException, URISyntaxException {
		return this.getPhenotypeCallByGeneAccessionAndFilter(accId, "");
	}

	@Override
	public PhenotypeFacetResult getPhenotypeCallByGeneAccessionAndFilter(
			String accId, String queryString) throws IOException, URISyntaxException {

		String solrUrl= config.get("internalSolrUrl");//"http://wwwdev.ebi.ac.uk/mi/solr/genotype-phenotype";
		String url =solrUrl+"/"+core+ "/select/?q=marker_accession_id:\""
				+ accId
				+ "\"&rows=10000000&version=2.2&start=0&indent=on&wt=json&facet=true&facet.field=resource_fullname&facet.field=top_level_mp_term_name";
			if (queryString.startsWith("&")) {
				url += queryString;
			} else {// add an ampersand parameter splitter if not one as we need
					// one to add to the already present solr query string
				url += "&" + queryString;
			}
		

		
				return createPhenotypeResultFromSolrResponse(url);
	}

	private PhenotypeFacetResult createPhenotypeResultFromSolrResponse(
			String url) throws IOException, URISyntaxException{
		PhenotypeFacetResult facetResult=new PhenotypeFacetResult();
		List<PhenotypeCallSummary> list = new ArrayList<PhenotypeCallSummary>();

		JSONObject results = null;
			results = JSONRestUtil.getResults(url);
	
		JSONArray docs = results.getJSONObject("response").getJSONArray(
				"docs");
		for (Object doc : docs) {
			JSONObject phen = (JSONObject) doc;
			String mpTerm = phen.getString("mp_term_name");
			String mpId = phen.getString("mp_term_id");
			PhenotypeCallSummary sum = new PhenotypeCallSummary();

			OntologyTerm phenotypeTerm = new OntologyTerm();
			phenotypeTerm.setName(mpTerm);
			phenotypeTerm.setDescription(mpTerm);
			DatasourceEntityId mpEntity=new DatasourceEntityId();
			mpEntity.setAccession(mpId);
			phenotypeTerm.setId(mpEntity);
			sum.setPhenotypeTerm(phenotypeTerm);
			if(phen.containsKey("allele_symbol")){
			Allele allele = new Allele();
			allele.setSymbol(phen.getString("allele_symbol"));
			GenomicFeature alleleGene=new GenomicFeature();
			DatasourceEntityId alleleEntity=new DatasourceEntityId();
			alleleEntity.setAccession(phen.getString("allele_accession_id"));
			allele.setId(alleleEntity);
			alleleGene.setId(alleleEntity);
			alleleGene.setSymbol(phen.getString("marker_symbol"));
			allele.setGene(alleleGene);
			sum.setAllele(allele);
			}
			if(phen.containsKey("marker_symbol")){
				GenomicFeature gf = new GenomicFeature();
				gf.setSymbol(phen.getString("marker_symbol"));
				DatasourceEntityId geneEntity=new DatasourceEntityId();
				geneEntity.setAccession(phen.getString("marker_accession_id"));
				gf.setId(geneEntity);
				sum.setGene(gf);
				}
			
			// GenomicFeature gene=new GenomicFeature();
			// gene.
			// allele.setGene(gene);
			String zygosity = phen.getString("zygosity");
			ZygosityType zyg = ZygosityType.valueOf(zygosity);
			sum.setZygosity(zyg);
			String sex = phen.getString("sex");
			SexType sexType = SexType.valueOf(sex);
			sum.setSex(sexType);
			String provider = phen.getString("resource_fullname");
			Datasource datasource = new Datasource();
			datasource.setName(provider);
			sum.setDatasource(datasource);
		
//				"parameter_stable_id":"557",
//		        "parameter_name":"Bone Mineral Content",
//		        "procedure_stable_id":"41",
//		        "procedure_stable_key":"41",
			Parameter parameter=new Parameter();
			parameter.setStableId(phen.getString("parameter_stable_id"));
			parameter.setName(phen.getString("parameter_name"));
			parameter.setStableKey(Integer.parseInt(phen.getString("procedure_stable_key")));
			sum.setParameter(parameter);
			Project project=new Project();
			project.setName(phen.getString("project_name"));
			project.setDescription(phen.getString("project_fullname"));//is this right for description? no other field in solr index!!!
			if(phen.containsKey("external_id")){
			sum.setExternalId(phen.getInt("external_id"));
			}
			sum.setProject(project);
//				 "procedure_stable_id":"77",
//			        "procedure_stable_key":"77",
//			        "procedure_name":"Plasma Chemistry",
			Procedure procedure=new Procedure();
			procedure.setStableId(phen.getString("procedure_stable_id"));
			procedure.setStableKey(Integer.valueOf(phen.getString("procedure_stable_key")));
			procedure.setName(phen.getString("procedure_name"));
			sum.setProcedure(procedure);
			list.add(sum);
		}

		//if (!filterString.equals("")) {//only run facet code if there is a facet in the query!!
		//get the facet information that we can use to create the buttons / dropdowns/ checkboxes
		JSONObject facets = results.getJSONObject("facet_counts")
				.getJSONObject("facet_fields");
		Iterator<String> ite = facets.keys();
		Map<String, Map<String, Integer>> dropdowns = new HashMap<String, Map<String, Integer>>();
		while (ite.hasNext()) {
			Map<String, Integer> map = new HashMap<String, Integer>();
			String key = (String) ite.next();
			JSONArray array = (JSONArray) facets.get(key);
			int i = 0;
			while (i +1< array.size()) {
				String facetString = array.get(i).toString();
				int number = array.getInt(i + 1);
				if (number != 0) {// only add if some counts to filter on!
					map.put(facetString, number);
				}
				i += 2;
				//System.out.println("i="+i);
			}
			dropdowns.put(key, map);
		}
		facetResult.setFacetResults(dropdowns);
		
		//}
		facetResult.setPhenotypeCallSummaries(list);
		return facetResult;
	}

	@Override
	public PhenotypeFacetResult getPhenotypeCallByMPAccession(
			String phenotype_id) throws IOException, URISyntaxException {
		return this.getPhenotypeCallByMPAccessionAndFilter(phenotype_id, "");
	
	}

	@Override
	public PhenotypeFacetResult getPhenotypeCallByMPAccessionAndFilter (
			String phenotype_id, String queryString) throws IOException, URISyntaxException {
		// http://wwwdev.ebi.ac.uk/mi/solr/genotype-phenotype/select/?q=mp_term_id:MP:0010025&rows=100&version=2.2&start=0&indent=on&defType=edismax&wt=json&facet=true&facet.field=resource_fullname&facet.field=top_level_mp_term_name&
		String solrUrl= config.get("internalSolrUrl");//"http://wwwdev.ebi.ac.uk/mi/solr/genotype-phenotype";
		String url = solrUrl+"/"+core+"/select/?q=mp_term_id:\""
				+ phenotype_id
				+ "\"&rows=1000000&version=2.2&start=0&indent=on&wt=json&facet=true&facet.field=resource_fullname&facet.field=procedure_name";
		//if (!filterString.equals("")) {
			if (queryString.startsWith("&")) {
				url += queryString;
			} else {// add an ampersand parameter splitter if not one as we need
					// one to add to the already present solr query string
				url += "&" + queryString;
			}
		//}

				return createPhenotypeResultFromSolrResponse(url);
			
			
	}

}
