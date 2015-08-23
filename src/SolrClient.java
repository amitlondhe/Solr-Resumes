import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;

public class SolrClient {
	static final List<String> UNIGRAM_SKILL_REFERENCE_LIST = Arrays.asList("java","ejb","soap","jsp","servlet","ooad","ibatis","hibernate","jpa"
			,"orm","webservice","axis","jax-ws","jax-rs","restful","restlet","wsdl","javascript","dojo","yui","jquery","struts","tiles","junit",
			"spring","tomcat","weblogic","websphere","was","mq","uml","json","oracle","mysql","esql","sql","");
	static final List<String> BIGRAM_SKILL_REFERENCE_LIST = Arrays.asList("java bean","websphere mq","rational rose","message broker","session bean",
			"design patterns","apache poi","adobe flex");
	static final List<String> TRIGRAM_SKILL_REFERENCE_LIST = Arrays.asList("enterprise java bean","message driven bean","stateless session bean",
			"stateful session bean");
	
	public static void main(String args[]) throws Exception {
		// readDocument();
		String str = "please divide this sentence into shingles";
		parseDocument(str);
		// addDocument();
	}

	private static void parseDocument(String doc) throws IOException {
		List<String> biGramList = parseDocumentForNGrams(doc,2);
		List<String> triGramList = parseDocumentForNGrams(doc,3);
	}

	private static List<String> parseDocumentForNGrams(String doc,int shingleSize) throws IOException {
		List<String> nGramList = new ArrayList<String>();
		ShingleAnalyzerWrapper analyzer = new ShingleAnalyzerWrapper(
				new WhitespaceAnalyzer(), shingleSize, shingleSize, " ", false, false, "_");
		TokenStream stream = null;
		try {
			stream = analyzer.tokenStream("dummy", doc);
			stream.reset();
			while (stream.incrementToken()) {
				if (stream.hasAttribute(TypeAttribute.class)) {
					TypeAttribute type = stream
							.getAttribute(TypeAttribute.class);
					if (type.type().equals("shingle")) {
						CharTermAttribute attr = stream
								.getAttribute(CharTermAttribute.class);
						//System.out.println(attr);
						nGramList.add(attr.toString());
					}
				}
			}
			System.out.println(nGramList);
		} finally {
			analyzer.close();
			if (stream != null) {
				stream.end();
				stream.close();
			}
		}
		return nGramList;
	}
	
	static class MyAnalyzer extends Analyzer {
		@Override
		protected TokenStreamComponents createComponents(String fieldName,
				Reader reader) {
		    final Tokenizer source = new LowerCaseTokenizer(reader);
		    StopFilter stopFilter = new StopFilter(source, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
		    return new TokenStreamComponents(source, stopFilter);		
		}
	}

	private static void addDocument() {
		try {
			HttpSolrServer server = new HttpSolrServer(
					"http://localhost:8983/solr/");
			SolrPingResponse pingResponse = server.ping();
			System.out.println(pingResponse.getStatus());

			SolrInputDocument doc = new SolrInputDocument();
			doc.addField("id", "tsetstst3r4", 1.0f);
			doc.addField("name", "doc1", 1.0f);
			doc.addField("price", 10);
			server.add(doc);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void readDocument() throws Exception {
		File f = new File("data");
		System.out.println(f.getCanonicalPath());
		String[] contents = f.list();
		for (String file : contents) {
			System.out.println("file" + file);
			File f1 = new File("data/" + file);
			InputStream stream = new FileInputStream(f1);
			Tika tika = new Tika();
			Metadata metadata = new Metadata();
			try {
				String doc = tika.parseToString(stream, metadata);
				parseDocument(doc);
				System.out.println(doc.substring(0, 50));
				for (String data : metadata.names()) {
					// System.out.println("Metadata Property - " + data + " - "
					// + metadata.get(data));
				}
			} finally {
				stream.close();
			}
		}
	}

}
