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
* Sequence region data access manager implementation.
* 
* @author Gautier Koscielny (EMBL-EBI) <koscieln@ebi.ac.uk>
* @since February 2012
*/

import java.util.HashMap;
import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.phenotype.pojo.SequenceRegion;



@Service
public class SequenceRegionDAOImpl extends HibernateDAOImpl implements
		SequenceRegionDAO {

	@Autowired
	private SessionFactory sessionFactory;
	
	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public List<SequenceRegion> getAllSequenceRegions() {
		return getCurrentSession().createQuery("from SequenceRegion").list();
	}

	@Transactional(readOnly = true)
	public SequenceRegion getSequenceRegionByName(String name) {
		return (SequenceRegion) getCurrentSession().createQuery("from SequenceRegion as region where region.name= ?").setString(0, name).uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public HashMap<String, SequenceRegion> getSequenceRegionsMap() {
		HashMap<String, SequenceRegion> map = new HashMap<String, SequenceRegion>();
		List<SequenceRegion> l = getCurrentSession().createQuery("from SequenceRegion").list();
		for (SequenceRegion r: l) {
			map.put(r.getName(), r);
		}
		return map;
	}

}
