package com.sesar.dao;

import java.util.ArrayList;
import java.util.List;

import com.sesar.model.*;
import com.sesar.util.DatabaseUtil;
/**
* Retrieve data from XML and create updating queries for saving to sampling_feature, specimen, sampling_feature_external_identifier, related_feature, taxonomic_classifier, sampling_feature_taxonomic_classifier, annotation and sampling_feature_annotation of database.
*
* @author  Bai
* @version 1.0
* @since   2017-07-11 
*/

public class SamplingFeatureDao {

	private Sample sample;
	private int sfNum; //sampling_feature_num
	private List<String> queries = new ArrayList<String>();
	private int annotationNum;
	private int sfAnnotationNum;
//	private int sfepBridgeNum; //sampling_feature_extension_property.bridge_num
//	private int foiNum; //feature_of_interest number
//	private int foiCvNum; //feature_of_interest_cv number
//	private int foiTypeNum; //max feature_of_interest_type number
//	private int maxMethodNum; 
//	private int collMethodNum; //collection_method
	
	public SamplingFeatureDao (Sample sample) {
		this.sample= sample;
	/*	Object obj = DatabaseUtil.getUniqueResult("select max(sampling_feature_num) from sampling_feature");
		if(obj != null) sfNum = (Integer)obj; 
		else sfNum = 0; */
	}
	
	public String saveDataToDB() {		
		String error = saveSamplingFeature();
		if(error==null) error=saveSpecimen();
		else return error;
		if(error==null) error=saveSamplingFeatureExternalIdentifierForIGSN();
		else return error;
		if(error==null) error=saveSamplingFeatureTaxonomicClassifierForMetamorphic();
		else return error; 

		//SamplingFeatureAnnotation
//		//annotationNum =  (Integer)DatabaseUtil.getUniqueResult("select max(annotation_num) from annotation");		
		//sfAnnotationNum = (Integer)DatabaseUtil.getUniqueResult("SELECT max(sampling_feature_annotation_num) FROM sampling_feature_annotation");
		String annotation = sample.getClassificationComment();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"SampleComment");
		annotation = sample.getFieldName();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"SpecimenFieldName");		
		annotation = sample.getGeologicalAge();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"GeologicAge");			
		annotation = sample.getSampleComment();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"SampleComment");		
		annotation = sample.getNavigationType();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"location navigation type");
		annotation = sample.getLocationDescription();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"location description");
		annotation = sample.getLocalityDescription();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"locality description");
		annotation = sample.getCountry();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"country");
		annotation = sample.getProvince();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"province");
		annotation = sample.getCounty();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"county");
		annotation = sample.getCity();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"city");		
		annotation = sample.getCurrentArchive();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"current archive");
		annotation = sample.getCurrentArchiveContact();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"current archive contact");
		annotation = sample.getOriginalArchive();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"original archive");
		annotation = sample.getOriginalArchiveContact();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"original archive contact");
		annotation = sample.getDepthScale();
		if(!"".equals(annotation)) saveSamplingFeatureAnnotation(annotation,"depth spatial reference system");
		SampleOtherNames sons = sample.getSampleOtherNames();
		List<String> annotationList = sons.getSampleOtherName();
		for(String an: annotationList) {
			if(!"".equals(an)) saveSamplingFeatureAnnotation(an,"Other name");
		}
		Integer typeNum = (Integer)DatabaseUtil.getUniqueResult("select annotation_type_num from annotation_type where annotation_type_name = 'related resource link'");
		ExternalUrls externalUrls = sample.getExternalUrls();
		List<ExternalUrl> urls = externalUrls.getExternalUrl();
		for(ExternalUrl ex: urls) saveSamplingFeatureAnnotationForExternalUrl(ex, typeNum);
		
		
		
		//SamplingFeatureExtensionProperty
		//Object obj = DatabaseUtil.getUniqueResult("SELECT max(bridge_num) FROM sampling_feature_extension_property");		
		//if(obj != null) sfepBridgeNum = (Integer) obj;
		Float extensionProperty = sample.getAgeMin();
		if(extensionProperty != null) saveSamplingFeatureExtensionProperty(extensionProperty,"minimum numeric age");
		extensionProperty = sample.getAgeMax();
		if(extensionProperty != null) saveSamplingFeatureExtensionProperty(extensionProperty,"maximum numeric age");
		extensionProperty = sample.getSize();
		if(extensionProperty != null) {
			String sizeUnit = sample.getSizeUnit();
			if(!"".equals(sizeUnit))
			saveSamplingFeatureExtensionProperty(extensionProperty,"sample_size_"+sizeUnit);
		}
		extensionProperty = sample.getDepthMin();
		if(extensionProperty != null) saveSamplingFeatureExtensionProperty(extensionProperty,"depth, minimum");
		extensionProperty = sample.getDepthMax();
		if(extensionProperty != null) saveSamplingFeatureExtensionProperty(extensionProperty,"depth, maximum");
		extensionProperty = sample.getPurpose();
		if(extensionProperty != null) saveSamplingFeatureExtensionProperty(extensionProperty,"sample purpose");
		
		//FeatureOfInterest
//		foiNum = (Integer)DatabaseUtil.getUniqueResult("SELECT max(feature_of_interest_num) FROM feature_of_interest");	
//		foiCvNum = (Integer)DatabaseUtil.getUniqueResult("SELECT max(feature_of_interest_cv_num) FROM feature_of_interest_cv");		
//		foiTypeNum = (Integer)DatabaseUtil.getUniqueResult("SELECT max(feature_of_interest_type_num) FROM feature_of_interest_type");		
		String gu = sample.getGeologicalUnit();
		if(!"".equals(gu)) saveFeatureOfInterest(gu, "GEOLOGICAL_UNIT"); 
		String pName = sample.getPrimaryLocationName();
		String pType = sample.getPrimaryLocationType();
		if(!"".equals(pName) && !"".equals(pType)) saveFeatureOfInterest(pName, pType);
		String locality = sample.getLocality();
		if(!"".equals(locality)) saveFeatureOfInterest(locality, "LOCALITY");
		new ActionDao(sample, sfNum, queries).saveData();		
		if(error == null) error = DatabaseUtil.update(queries);			
		return error;
	}

	
	private String saveSamplingFeature() {
		String name = sample.getName();
		Object obj =  DatabaseUtil.getUniqueResult("select sampling_feature_code from sampling_feature s where upper(sampling_feature_code) = upper('"+name+"')");
		if(obj != null) return "Sample name "+name+ " already exists in database";
		String type = sample.getSampleType();
		obj = DatabaseUtil.getUniqueResult("select sampling_feature_type_num from sampling_feature_type where upper(sampling_feature_type_name) = upper('"+type+"')");
		if(obj == null) return "sample_type: "+type+" is not found in database";
		int typeNum = (Integer)obj;
		String geometry = getGeometry(sample.getStartPoint(), sample.getEndPoint());
		sfNum = (Integer)DatabaseUtil.getUniqueResult("select nextval('sampling_feature_sampling_feature_num_seq')");
		String q =  "INSERT INTO sampling_feature values ("+sfNum+","+typeNum+",'"+name+"',null,'"+sample.getDescription()+"',"+geometry+sample.getElevationM()+",'"+sample.getVerticalDatum()+"')";
		queries.add(q);	
		return null;
	}
	
	private String saveSpecimen() {
		String material = sample.getMaterial();
		if(material == null) return null;
		if(material.equals("Mineral")) material = "MIN";
		Object obj =  DatabaseUtil.getUniqueResult("select material_num from material where upper(material_code) = upper('"+material+"')");
		if(obj == null) return "Material "+material+ " is not found in database";
		String q ="INSERT INTO specimen VALUES ("+sfNum+","+obj+",'f')";
		queries.add(q);
		return null;
	}
	
	private String saveSamplingFeatureExternalIdentifierForIGSN() {
		String ic = sample.getIgsn();
		//Object obj =  DatabaseUtil.getUniqueResult("select max(bridge_num) from sampling_feature_external_identifier");
		//int bridgeNum = 1;
		//if(obj != null ) bridgeNum = (Integer)obj+1;
		String q ="INSERT INTO sampling_feature_external_identifier VALUES (nextval('sampling_feature_external_identifier_bridge_num_seq'),"+sfNum+",upper('"+ic+"'),null,2)";
		queries.add(q); 	
		String ip = sample.getParentIgsn();
		if(ip != null && !"".equals(ip)) {
			Object obj =  DatabaseUtil.getUniqueResult("select sample_feature_num from sampling_feature_external_identifier where sampling_feature_external_id = upper('"+ip+"')");
			if(obj == null) return "Parent IGSN "+ip + " is not found in database";
			ip = ""+obj;
		//	obj= DatabaseUtil.getUniqueResult("select max(related_feature_num+1) from related_feature");
			q = "INSERT INTO related_feature VALUES (nextval('related_feature_related_feature_num_seq'),"+sfNum+",13," +ip+")";		
			queries.add(q); 
		} 
		return null;
	}
	
	private String saveSamplingFeatureTaxonomicClassifierForMetamorphic() {
		Classification classification = sample.getClassification();
		Rock rock = classification.getRock();
		rock.getMetamorphic().getMetamorphicType();
		String type = rock.getMetamorphic().getMetamorphicType();
		if("".equals(type)) return null;
		//Object obj =  DatabaseUtil.getUniqueResult("select max(taxonomic_classifier_num+1) from taxonomic_classifier");
		Integer tcNum = (Integer)DatabaseUtil.getUniqueResult("select nextval('taxonomic_classifier_taxonomic_classifier_num_seq')");
		String q ="INSERT INTO taxonomic_classifier VALUES ("+tcNum +",'Sesar','"+type+"',null,'Sesar Classification from Sesar')";
		queries.add(q);
	//	obj =  DatabaseUtil.getUniqueResult("SELECT max(bridge_num+1) FROM sampling_feature_taxonomic_classifier");
		q ="INSERT INTO sampling_feature_taxonomic_classifier VALUES (nextval('sampling_feature_taxonomic_classifier_bridge_num_seq'),"+sfNum+","+tcNum+")";
		queries.add(q);
		return null;
	}
	
	private void saveSamplingFeatureAnnotationForExternalUrl(ExternalUrl ex, Integer typeNum) {
		String text = ex.getDescription();
		if(!"".equals(text)) {
			String code = ex.getUrlType();
			String link = ex.getUrl();
			annotationNum = (Integer) DatabaseUtil.getUniqueResult("select nextval('annotation_annotation_num_seq')");
			String q = "INSERT INTO annotation values ("+annotationNum+","+typeNum+",'"+text+"',139,now(),'"+link+"','"+code+"')";
			queries.add(q);
			sfAnnotationNum = (Integer)DatabaseUtil.getUniqueResult("select nextval('sampling_feature_annotation_sampling_feature_annotation_num_seq')");
			q = "INSERT INTO sampling_feature_annotation values ("+sfAnnotationNum +","+sfNum+","+annotationNum+")";
			queries.add(q);
		}
	}
	
	/////////////
	private void saveSamplingFeatureAnnotation(String text, String type) {
		Integer typeNum = (Integer) DatabaseUtil.getUniqueResult("select annotation_type_num from annotation_type where annotation_type_name = '"+type+"'");
		annotationNum = (Integer) DatabaseUtil.getUniqueResult("select nextval('annotation_annotation_num_seq')");
		String q = "INSERT INTO annotation values ("+annotationNum+","+typeNum+",'"+text+"',139,now())";
		queries.add(q);
		sfAnnotationNum = (Integer)DatabaseUtil.getUniqueResult("select nextval('sampling_feature_annotation_sampling_feature_annotation_num_seq')");
		q = "INSERT INTO sampling_feature_annotation values ("+sfAnnotationNum+","+sfNum+","+annotationNum+")";
		queries.add(q);
	}
	
	private void saveSamplingFeatureExtensionProperty(Float value, String type) {
		Integer typeNum = (Integer) DatabaseUtil.getUniqueResult("SELECT extension_property_num FROM extension_property where extension_property_name = '"+type+"'");
		String q = "INSERT INTO sampling_feature_extension_property values (nextval('sampling_feature_extension_property_bridge_num_seq'),"+sfNum+","+typeNum+","+value+")";
		queries.add(q);
	}
	
	private void saveFeatureOfInterest(String cvName, String type) {
		Object obj = DatabaseUtil.getUniqueResult("SELECT feature_of_interest_cv_num FROM feature_of_interest_cv where feature_of_interest_cv_name = '"+cvName+"'");
		Integer cvNum = null;
		if(obj != null) cvNum = (Integer)obj;
		else {
			cvNum = (Integer)DatabaseUtil.getUniqueResult("select nextval('feature_of_interest_cv_feature_of_interest_cv_num_seq')");
			queries.add("INSERT INTO feature_of_interest_cv values ("+cvNum+",'"+cvName+"','SESAR')");			
		}
		Integer typeNum = null;
		obj = DatabaseUtil.getUniqueResult("SELECT feature_of_interest_type_num FROM feature_of_interest_type where feature_of_interest_type_name = '"+type+"'");
		if(obj != null) typeNum = (Integer)obj;
		else {
			typeNum = (Integer) DatabaseUtil.getUniqueResult("select nextval('feature_of_interest_type_feature_of_interest_type_num_seq')");
			queries.add("INSERT INTO feature_of_interest_type values ("+typeNum+",'"+type+"','primary_location_name, sesar')");
		}
		
		queries.add("INSERT INTO feature_of_interest values (nextval('feature_of_interest_feature_of_interest_num_seq'),"+sfNum+","+typeNum+","+cvNum+")");
	}
	
	private String getGeometry(String p1, String p2) {
   	 if(p2 != null && p1 != null) {return "'LINE' ,ST_SetSRID(ST_MakeLINE(ST_MakePoint("+p1+"), ST_MakePoint("+p2+")), 4326),";  		
   	 } else if (p1 != null) {
   		 return "'POINT',ST_SetSRID(ST_MakePoint("+p1+"), 4326),";
   	 } else {
   		 return "'N/A',ST_SetSRID(ST_MakePoint(90,0), 4326),";
   	 }
    }

	
}
