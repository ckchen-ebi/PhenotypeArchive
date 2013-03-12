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
 * 
 * genomicB.js
 * Contains utility methods for the phenotype archive gene details page
 * 
 */
// Convert a URL to HTTPS if necessary
function convertToHttps(url){
	var protocol = location.protocol;
	if(protocol=='https:'){
		url=url.replace('http:', 'https:');
	}
	return url;
}

// Transform original url into a url that we proxy through to avoid
// CORS errors and get round ajax permission restrictions
function getProxyUri(originalUrl) {
	
	var root = location.protocol + '//' + location.host;
	var localUrl = originalUrl;

	//if on localhost just return the original url
	if(root == 'http://localhost:8080' || root == 'https://localhost:8080') {
		return originalUrl;
	}else{
		if(originalUrl.indexOf('http://www.ebi.ac.uk/mi/ws/dazzle-ws/das/') != -1){
			localUrl = originalUrl.replace('http://www.ebi.ac.uk/mi/ws/dazzle-ws/das/', root + '/mi/ws/dazzle-ws/das/');
		}
//		if(originalUrl.indexOf('http://www.derkholm.net:9080/das/') != -1){
//			localUrl = originalUrl.replace('http://www.derkholm.net:9080/das/', root + '/derkholm/das/');
//		}
		if(originalUrl.indexOf('http://beta.mousephenotype.org/mi/ws/das-ws/das/ikmcallelesm38/') != -1){
			localUrl = originalUrl.replace('http://beta.mousephenotype.org/mi/ws/das-ws/das/ikmcallelesm38/',root + '/mi/ws/das-ws/das/ikmcallelesm38/');
		}
		if(originalUrl.indexOf("http://gbrowse.informatics.jax.org/cgi-bin/gbrowse_img/thumbs_current") != -1){
			localUrl = originalUrl.replace('http://gbrowse.informatics.jax.org/cgi-bin/gbrowse_img/thumbs_current',root + '/jax/cgi-bin/gbrowse_img/thumbs_current/');
		}
	}
	localUrl = convertToHttps(localUrl);
	return localUrl;
}

// Document onload functions 
jQuery(document).ready(function() {


	// See here for this solution to detecting IE. 
	// http://stackoverflow.com/questions/4169160/javascript-ie-detection-why-not-use-simple-conditional-comments
	function supportsSvg(){
		var isIE = /*@cc_on!@*/false;
		if(isIE){
			return false;
		}else{
			return true;
		}
	}
	
	//$("#showGBrowser").click(function()  {
						var chromosome = $('#chr').html();
					var start = parseInt($('#geneStart').html());
					var stop = parseInt($('#geneEnd').html());

					// If reverse strand, swap the coordinates
//					if(stop<start){
//						start = doc.coord_end;
//						stop = doc.coord_start;
//					}
					
					if(!supportsSvg()){
						
						// This browser cannot support the interactive browser

						var dontUseIEString='<div class="alert alert-info">For a more interactive and informative gene image please use a newish browser e.g. <a href="http://www.mozilla.com/firefox/">Firefox</a> 3.6+, <a href="http://www.google.com/chrome">Google Chrome</a>, and <a href="http://www.apple.com/safari/">Safari</a> 5 or newer</div>';
						var gbrowseimage='<a href="http://gbrowse.informatics.jax.org/cgi-bin/gb2/gbrowse/mousebuild38/?start='+start+';stop='+stop+';ref='+chromosome+'"><img border="0" src="http://gbrowse.informatics.jax.org/cgi-bin/gb2/gbrowse_img/mousebuild38/?t=MGI_Genome_Features;name='+chromosome+':'+start+'..'+stop+';width=400"></a>';
						$('#svgHolder').html(dontUseIEString+gbrowseimage);
						forceWidth: jQuery('div.row-fluid').width() * 0.98;

						// Remove the the info bar and help text about the genome 
						// browser being interactive because the IE fallback 
						// browser (gbrowse) isn't interactive
						$('#genomicBrowserInfo').html('');
					} else {
					
						// Display the interactive browser

						var b= new Browser({
							chr:        chromosome,
							viewStart:  start-1000,
							viewEnd:    stop+1000,
							noPersist: true,
							coordSystem: {
								speciesName: 'Mouse',
								taxon: 10090,
								auth: 'NCBIM',
								version: 38
							},
//							chains: {
//								mm8ToMm9: new Chainset(getProxyUri('http://www.ebi.ac.uk/mi/ws/dazzle-ws/das/mm8ToMm9/'), 'NCBIM36', 'NCBIM37', {
//									speciesName: 'Mouse',
//									taxon: 10090,
//									auth: 'NCBIM',
//									version: 36
//								})
//							},
							sources: [
							{
								name: 'Genome',
								uri: getProxyUri( 'http://www.ebi.ac.uk/mi/ws/dazzle-ws/das/mmu_68_38k/'),
								desc: 'Mouse reference genome build NCBIm38',
								tier_type: 'sequence',
								provides_entrypoints: true
								},
								{
									name: 'Genes',
									desc: 'Gene structures from Ensembl 58',
									uri: getProxyUri('http://www.ebi.ac.uk/mi/ws/dazzle-ws/das/mmu_68_38k/'),
									collapseSuperGroups: true,
									provides_karyotype: true,
									provides_search: true
								},
								{
									name: 'ikmc alleles',     
									desc: 'ikmc alleles',
									uri: getProxyUri('http://beta.mousephenotype.org/mi/ws/das-ws/das/ikmcallelesm38/'),collapseSuperGroups: true   
								}
//								,
//								{
//									name: 'CpG',
//									desc: 'CpG observed/expected ratio',
//									uri: 'http://www.derkholm.net:9080/das/mm9comp/',
//									stylesheet_uri: 'http://www.derkholm.net/dalliance-test/stylesheets/cpg.xml'
//								}
							],
							searchEndpoint: new DASSource(getProxyUri('http://www.ebi.ac.uk/mi/ws/dazzle-ws/das/mmu_68_38k/')),
							karyoEndpoint: new DASSource(getProxyUri('http://www.ebi.ac.uk/mi/ws/dazzle-ws/das/mmu_68_38k/')),
							browserLinks: {
								Ensembl: 'http://www.ensembl.org/Mus_musculus/Location/View?r=${chr}:${start}-${end}',
								UCSC: 'http://genome.ucsc.edu/cgi-bin/hgTracks?db=mm10&position=chr${chr}:${start}-${end}'
							},
							forceWidth: jQuery('div.row-fluid').width() * 0.98
						}); //end var b= new Browser({
					} //end if(!supportsSvg()){...}else{
				
	//	}); //end of on click new dalliance function
	
	
});// end jQuery(function($) { 
