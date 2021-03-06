package test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.ws.rs.core.Response;

import com.github.opendevl.JFlat;
import org.apache.commons.io.FileUtils;
import org.json.*;
import org.json.simple.parser.ParseException;

import static com.google.gson.internal.bind.TypeAdapters.URL;

public class RelationsJson2Csv {

    public static String string;

    public static void main(String[] args) throws IOException, JSONException, ParseException {

        RESTClient restClient = RESTClient.newClient ( new URL ( "https://test01.orionic.com" ) );
        restClient.setHeaders ( "Authorization: Token token=" + "mmx-ZMd9yCSKVKrAR25rSVnw" );
        String appendedUrl = "api/v1/relations?dir=any&filter[relation_type_cd]=11,12,13&include=source.path,related.path&limit=500000&filter[source.origin_nm]=qlik_test1&filter[target.origin_nm]=qlik_test1";
        Response response = restClient.createBuilder ( appendedUrl ).get ();

        string = response.readEntity ( String.class );

        System.out.println ( string );
        File file1 = new File ( "C:/Users/IND029/Desktop/original_first.json" );
        FileUtils.writeStringToFile ( file1, string );

        System.out.println ( "writing" );
        JSONArray docs = null;
        try {
            JSONObject output = new JSONObject ( string );

            docs = output.getJSONArray ( "result" );

        } catch (JSONException e) {
            e.printStackTrace ();
        }
        File file2 = new File ( "C:/Users/IND029/Desktop/results_only_first.json" );
        FileUtils.writeStringToFile ( file2, docs.toString () );


        String jsonString = new String ( Files.readAllBytes ( Paths.get ( "C:/Users/IND029/Desktop/results_only_first.json" ) ), StandardCharsets.UTF_8 );

        JJFlat flatMe = new JJFlat ( jsonString );

        List <Object[]> json2csv = flatMe.json2Sheet ().getJsonAsSheet ();

        //directly write the JSON document to CSV
        flatMe.json2Sheet ().write2csv ( "C:/Users/IND029/Desktop/first_converted_output.csv" );

        System.out.println ( "written" );
    }

}




