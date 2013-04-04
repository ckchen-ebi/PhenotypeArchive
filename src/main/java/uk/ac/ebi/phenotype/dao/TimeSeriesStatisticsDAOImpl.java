package uk.ac.ebi.phenotype.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.phenotype.pojo.BiologicalModel;
import uk.ac.ebi.phenotype.pojo.CategoricalResult;
import uk.ac.ebi.phenotype.pojo.Organisation;
import uk.ac.ebi.phenotype.pojo.Parameter;
import uk.ac.ebi.phenotype.pojo.ParameterOption;
import uk.ac.ebi.phenotype.pojo.SexType;
import uk.ac.ebi.phenotype.pojo.TimeSeriesControlView;
import uk.ac.ebi.phenotype.pojo.ZygosityType;

public class TimeSeriesStatisticsDAOImpl extends HibernateDAOImpl implements TimeSeriesStatisticsDAO {

	public TimeSeriesStatisticsDAOImpl(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Transactional(readOnly = true)
	public Long hasEnoughData(SexType sex, ZygosityType zygosity, Parameter parameter, Integer populationId){
		return (Long) getCurrentSession()
			//.createQuery("SELECT COUNT(*) FROM CategoricalMutantView c WHERE c.sex=? AND c.zygosity=? AND c.parameter=? AND c.populationId=?")
			.createQuery("SELECT COUNT(*) FROM TimeSeriesMutantView c WHERE c.parameter=? AND c.populationId=?")
			//.setString(0, sex.name())
			//.setString(1, zygosity.name())
			.setLong(0, parameter.getId())
			.setInteger(1, populationId)
			.list()
			.get(0);
	}

	

	@Transactional(readOnly = true)
	public List<DiscreteTimePoint> countControl(SexType sex, Parameter parameter, Integer populationId){
		Connection connection = this.getSession().connection();
		 List<DiscreteTimePoint> timeData=new ArrayList<DiscreteTimePoint>();
		try {
			Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt.executeQuery("select * from stats_mv_control_time_series_values where sex='"+sex.name()+"' and  population_id="+populationId+" order by discrete_point");
			
			while(resultSet.next()){
				
				Float time=resultSet.getFloat("discrete_point");
				Float data=resultSet.getFloat("data_point");
				//System.out.println(time+" "+data);
				DiscreteTimePoint pt=new DiscreteTimePoint(time,data);
				pt.setData(data);
				timeData.add(pt);
			}
			
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return timeData;
	}

	@Transactional(readOnly = true)
	public  List<DiscreteTimePoint> countMutant(SexType sex, ZygosityType zygosity, Parameter parameter,  Integer populationId){
		
		
		Connection connection = this.getSession().connection();
		
		 List<DiscreteTimePoint> timeData=new ArrayList<DiscreteTimePoint>();
		try {
			Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt.executeQuery("select * from stats_mv_experimental_time_series_values  WHERE sex='"+sex.name()+"' AND zygosity='"+zygosity.name()+"' AND parameter_id=" + parameter.getId() +" AND population_id="+populationId+ " order by discrete_point");
			
			while(resultSet.next()){
				Float time=resultSet.getFloat("discrete_point");
				Float data=resultSet.getFloat("data_point");
				//System.out.println(time+" "+data);
				DiscreteTimePoint pt=new DiscreteTimePoint(time, data);
				timeData.add(pt);
			}
			
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return timeData;
	}

	@Transactional(readOnly = true)
	public BiologicalModel getControlBiologicalModelByPopulation(Integer populationId) {
		BiologicalModel bm = (BiologicalModel) getCurrentSession().createQuery("SELECT bs.biologicalModel FROM Observation o inner join o.sample as bs inner join bs.biologicalModel bm WHERE o.populationId=? AND bs.group='control'")
			.setInteger(0, populationId)
			.list()
			.get(0);
		return bm;
	}

	@Transactional(readOnly = true)
	public BiologicalModel getMutantBiologicalModelByPopulation(Integer populationId) {
		BiologicalModel bm = (BiologicalModel) getCurrentSession().createQuery("SELECT bs.biologicalModel FROM Observation o inner join o.sample as bs inner join bs.biologicalModel bm WHERE o.populationId=? AND bs.group='experimental'")
			.setInteger(0, populationId)
			.list()
			.get(0);
		return bm;
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public List<BiologicalModel> getBiologicalModelsByParameterAndGene(Parameter parameter, String accessionId) {
		List<BiologicalModel> bms = (List<BiologicalModel>) getCurrentSession().createQuery("SELECT DISTINCT c.biologicalModel FROM TimeSeriesMutantView c inner join c.biologicalModel as bm join bm.genomicFeatures as gf WHERE gf.id.accession=? AND c.parameter=?")
				.setString(0, accessionId)
				.setInteger(1, parameter.getId())
				.list();
		return bms;
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public List<BiologicalModel> getBiologicalModelsByParameter(Parameter parameter) {
		List<BiologicalModel> bms = (List<BiologicalModel>) getCurrentSession().createQuery("SELECT DISTINCT c.biologicalModel FROM TimeSeriesMutantView c inner join c.biologicalModel as bm join bm.genomicFeatures as gf WHERE  c.parameter=?")
				.setInteger(0, parameter.getId())
				.list();
		return bms;
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public List<Integer> getPopulationIdsByParameterAndMutantBiologicalModel(Parameter parameter, BiologicalModel biologicalModel) {
		return (List<Integer>) getCurrentSession().createQuery("SELECT DISTINCT populationId FROM TimeSeriesMutantView WHERE parameter=? AND biologicalModel=?")
				.setLong(0, parameter.getId())
				.setInteger(1, biologicalModel.getId())
				.list();
	}

	public Double getpValueByParameterAndBiologicalModelAndSexAndZygosity(Parameter parameter, BiologicalModel biologicalModel, SexType sex, ZygosityType zygosity) {
		return (Double) getCurrentSession().createQuery("SELECT DISTINCT populationId FROM TimeSeriesMutantView WHERE parameter=? AND biologicalModel=?")
				.setLong(0, parameter.getId())
				.setInteger(1, biologicalModel.getId())
				.uniqueResult();
	}

	public Double getMaxEffectSizeByParameterAndBiologicalModelAndSexAndZygosity(Parameter parameter, BiologicalModel biologicalModel, SexType sex, ZygosityType zygosity) {
		return (Double) getCurrentSession().createQuery("SELECT DISTINCT populationId FROM TimeSeriesMutantView WHERE parameter=? AND biologicalModel=?")
				.setLong(0, parameter.getId())
				.setInteger(1, biologicalModel.getId())
				.uniqueResult();
	}

	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public List<Integer> getPopulationIdsByParameter(Parameter parameter) {
		return getCurrentSession().createQuery("SELECT DISTINCT populationId FROM TimeSeriesMutantView WHERE parameter=?")
			.setLong(0, parameter.getId())
			.list();	
	}


	@Transactional(readOnly = true)
	public Organisation getOrganisationByPopulation(Integer populationId) {
		Organisation organisation = (Organisation) getCurrentSession().createQuery("SELECT DISTINCT c.organisation FROM TimeSeriesMutantView c WHERE c.populationId=?")
			.setInteger(0, populationId)
			.uniqueResult();
		return organisation;
	}


	/**
	 * return the SexType ENUM associated to this population
	 */
	@Transactional(readOnly = true)
	public SexType getSexByPopulation(Integer populationId) {
		SexType sex = (SexType) getCurrentSession().createQuery("SELECT DISTINCT c.sex FROM TimeSeriesMutantView c WHERE c.populationId=?")
				.setInteger(0, populationId)
				.uniqueResult();
		return sex;
	}

	/**
	 * return the ZygosityType ENUM associated to this population
	 */
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public List<ZygosityType> getZygositiesByPopulation(Integer populationId) {
		List<ZygosityType> zygosity = (List<ZygosityType>) getCurrentSession().createQuery("SELECT DISTINCT c.zygosity FROM TimeSeriesMutantView c WHERE c.populationId=?")
			.setInteger(0, populationId)
			.list();
		return zygosity;
	}


//	@Transactional(readOnly = false)
//	public void deleteCategoricalResultByParameter(Parameter parameter) throws HibernateException, SQLException {
//		Statement stmt = getCurrentSession().connection().createStatement();
//		stmt.executeUpdate("DELETE FROM stats_categorical_results WHERE parameter_id="+parameter.getId());
//		stmt.close();
//	}
//
//	@Transactional(readOnly = false)
//	public void saveCategoricalResult(CategoricalResult result) {
//		getCurrentSession().saveOrUpdate(result);
//		getCurrentSession().flush();
//	}

}
