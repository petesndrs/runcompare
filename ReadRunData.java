import java.io.IOException;
import java.io.StringReader;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.net.URI;
import javax.swing.text.html.HTMLEditorKit;

class ReadRunData
{
  static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36";
  
  class RunPageParserCallback extends HTMLEditorKit.ParserCallback {

    public void handleComment(char[] data,int pos){
		System.out.println("Found " + pos + ":" + data.length);
    }
  
  }
  
  private void run(){
    System.out.println("Run");

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create("https://www.parkrun.org.uk/dulwich/parkrunner/690790/"))
      .setHeader("User-Agent", USER_AGENT)
      .build();

    String responseString = "";
    try {
      HttpResponse<String> response = HttpClient
        .newBuilder()
        .build()
        .send(request, HttpResponse.BodyHandlers.ofString());
      System.out.println("HttpResponse " + response.statusCode());
      responseString = (String) response.body();
      System.out.println(responseString);
    } catch (IOException | InterruptedException ex) {
    }

    StringReader r = new StringReader(responseString);
    HTMLEditorKit.Parser parse = new RunPageParser().getParser();

    try {
    parse.parse(r, new RunPageParserCallback(), true);
    } catch (IOException ex) {
    }
  }

  public static void main(String args[])
  {
    ReadRunData readRunData = new ReadRunData();
    readRunData.run();
  }
}