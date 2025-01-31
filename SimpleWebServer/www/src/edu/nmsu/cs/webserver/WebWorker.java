package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/
//added an import for File, FileReader, and FileNotFoundException
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.imageio.ImageIO;

public class WebWorker implements Runnable
{
	//instanciated Date, File, dateTag, serverTag, and a boolean
	Date currDate;
	File test;
	File requested;
	String dateTag;
	String serverTag;
	String path;
	String type;
	boolean noRequest;
	boolean fileFound;   /*set to true automatically, false if 
                        requested file is not found*/

	private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
		currDate = new Date();
        test = new File("test.html");
        fileFound = true;
        noRequest = true;
      
      //tags that can be read and replaced with data
		dateTag = "<cs371date>";
		serverTag = "<cs371server>";
      
      //path a requested file
		path = "";
		
        //default type
        type = "nothing";
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			readHTTPRequest(is);
			writeHTTPHeader(os, checkType(type) );
			System.out.println("writeHeader works");
			writeContent(os);
			System.out.println("writeContent works");	
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private void readHTTPRequest(InputStream is) throws IOException
	{
		
		String line;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				System.err.println("Request line: (" + line + ")");
				
				if(line.contains("GET"))
				   path = line;

                if (line.length() == 0)
					break;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		
		if(path.length() > 15)
        {
            noRequest = false;
            String root = test.getCanonicalPath();
            root = root.substring(0, root.length() - 10);	

            //prints out and makes a usable file path 
            //out of the requested
            System.out.println(path);
            int startIndex = path.indexOf('/');
            int endIndex = path.indexOf('H');
            path = path.substring(startIndex, endIndex - 1);
            String requestPath = root + path;
            System.out.println(requestPath.trim());
            requested = new File(requestPath.trim());
                  
            type = path.substring(path.indexOf(".") , path.length());
            System.out.println("Request type: " + type);
                    
        }
		
		else 
            noRequest = true;
		
		return;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		//added a File object that extracts the path of the file from the URL and checks if it exists
		//sends "404 not found" if it does not
      
        //only goes into the if statement if 
        //the browser is actually requesting a file
        
        if(!noRequest && !requested.canRead())
        {
            os.write("HTTP/1.1 404 Not Found\n".getBytes());
            fileFound = false;
        }
        
        else
            os.write("HTTP/1.1 200 OK\n".getBytes());
        
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Diego's Insane Radical and totally Awesome Server\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os) throws Exception, FileNotFoundException 
	{
        String line = "";
        File img = new File("propane.jpg");
        
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
        FileReader fr = new FileReader(test);
      	BufferedReader br = new BufferedReader(fr);
		
		if(noRequest)
		{
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h3>My web server works!</h3>\n".getBytes());
			os.write("</body></html>\n".getBytes());
			

			//while loop that prints out the contents of "test.html" and replaces date and server tags
			//with the current date and server
			while((line = br.readLine()) != null)
			{	
				if(line.contains(dateTag))
					line = line.replace(dateTag, (df.format(currDate)));
	   			
				

				if(line.contains(serverTag))
				   	line = line.replace(serverTag, "Diego's Insane Server");
			   		
                System.out.println(test.toString());
				os.write(line.getBytes());
			}
		
        }
        
        //there was a request, then checks if the requested file was found or not
		else 
		{
            //if file was found, checks if the file is an image or a text file
			if(fileFound)
			{
				type = checkType(type);
				if( type.equals("image/png") || type.equals("image/jpg") || type.equals("image/gif")) 
				{
                    byte[] imgData = Files.readAllBytes(requested.toPath());
                    os.write(imgData);
				}
				
				else
				{
                    String read = "";
                    FileReader filer = new FileReader(requested.getCanonicalPath());
                    BufferedReader bufferedr = new BufferedReader(filer);
                
                    while((read = bufferedr.readLine()) != null)
                    {
                        if(read.contains(dateTag))
                            read = read.replace(dateTag, (df.format(currDate)));
            
                        if(read.contains(serverTag))
                            read = read.replace(serverTag, "Diego's Insane Server");
                        
                        os.write(read.getBytes());
                        //os.write(imageData);
                    }
				}
			
			}

            else if(!fileFound)
	      	{
		 		os.write("<html><head></head><body>\n".getBytes());
				os.write("<h3>Error 404: File Not Found :(</h3>\n".getBytes());
				os.write("</body></html>\n".getBytes());

	      	}
		}
		
		//os.write(imgData);
		
	}
	
	/* Checks the type of the file thatwas requested. Called in "run"
	 *
     * @param String s, the string that is to get checked
	 */
	public String checkType(String t)
	{
        if(t.contains(".png"))
            return "image/png";
	
        if(t.contains(".jpeg") || t.contains(".jpg"))
            return "image/jpg";
            
        if(t.contains(".gif"))
            return "image/gif";
            
        return "text/html";
	}
	
} // end class
