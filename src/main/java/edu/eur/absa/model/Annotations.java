//package edu.eur.absa.model;
//
//import java.util.HashMap;
//
//@Deprecated
//public class Annotations extends HashMap<String, Object>{
//
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = -8058150766908873877L;
//	private Dataset dataset;
//	
//	@Deprecated
//	public Annotations(Dataset dataset){
//		this.dataset = dataset;
//	}
//	@Deprecated
//	public Annotations(HashMap<String, Object> annotations, Dataset dataset){
//		super.putAll(annotations);
//		this.dataset = dataset;
//	}
//	
////	@Deprecated
////	@SuppressWarnings("unchecked")
////	public <T> T get(String annotationType, Class<T> dataType){
////		Object value = get(annotationType);
////		if (value == null)
////			return null;
////		if (dataType.isInstance(value)){
////			return (T)value;
////		} else {
////			throw new ClassCastException("The annotation you requested is not of the type you specified");
////		}
////		
////	}
//	
////	@Deprecated
////	@SuppressWarnings("unchecked")
////	public <T> T get(String annotationType){
////		Class<T> classT = (Class<T>) dataset.getAnnotationDataTypes().get(annotationType);
////		return get(annotationType, classT);
////	}
//	
////	@Deprecated
////	public String getEntryText(String annotationType){
////		return annotationType+": "+get(annotationType).toString();
////	}
//	
////	@Deprecated
////	@Override
////	public Object put(String annotationType, Object value){
////		if (!dataset.getAnnotationDataTypes().containsKey(annotationType))
////			dataset.getAnnotationDataTypes().put(annotationType, value.getClass());
////		return put(annotationType, dataset.getAnnotationDataTypes().get(annotationType).cast(value));
////	}
//	
//}
