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
package uk.ac.ebi.phenotype.dao;

/**
 * 
 * Phenotype call manager interface
 * 
 * @author Gautier Koscielny (EMBL-EBI) <koscieln@ebi.ac.uk>
 * @since May 2012
 */

import java.util.List;

import uk.ac.ebi.phenotype.pojo.PhenotypeCallSummary;

public interface PhenotypeCallSummaryDAO {

	/**
	 * Get all phenotype call summaries
	 * @return all phenotype call summaries
	 */
	public List<PhenotypeCallSummary> getAllPhenotypeCallSummaries();

	/**
	 * Find all phenotype call summaries by accession id
	 * @param accId the accession ID associated to the call
	 * @return a list of matching phenotype calls
	 */
	public List<PhenotypeCallSummary> getPhenotypeCallByAccession(String accId, int dbId);

	/**
	 * Find all phenotype call summaries by MP accession id and external_db_id
	 * @param accId the MP accession ID associated to the call
	 * @param dbId is the external db id of MP
	 * @return a list of matching phenotype calls
	 */
	public List<PhenotypeCallSummary> getPhenotypeCallByMPAccession(String accId, int dbId);
}
