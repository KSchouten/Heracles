package edu.eur.absa.nlp;

import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;

public class LowercaseChanger extends AbstractNLPComponent {

	public LowercaseChanger(){
		this.thisTask = NLPTask.LOWERCASING;
		this.prerequisites.add(NLPTask.TOKENIZATION);
		//advised: also ONTOLOGY_LOOKUP
	}
	
	@Override
	public void validatedProcess(Dataset dataset, String spanType) {
		
		for (Span span : dataset.getSpans(spanType)){
			for (Word w : span){
				
				if (!w.hasAnnotation("URI"))
					w.setWord(w.getWord().toLowerCase());
			}
		}

	}

}
