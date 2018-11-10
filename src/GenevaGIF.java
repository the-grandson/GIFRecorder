import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadataNode;

public class GenevaGIF extends JFrame implements KeyListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final int FRAME_LIMIT = 1000;
	public JLabel label = null;
	public BlockingQueue<ImageIcon> roller = new ArrayBlockingQueue<ImageIcon>(FRAME_LIMIT);
	public Future<Boolean> loadGIFs;
	public final ExecutorService executor;
	public int SIZE_LIMIT;

	public GenevaGIF() throws Exception{
		super("Test Geneva GIF");
		this.setBounds(300, 200, 700, 400);
		this.label = new JLabel("Geneva GIF Displayer",
				SwingConstants.CENTER);
		this.add(this.label);
		this.setVisible(true);
		addKeyListener(this);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				System.exit(0);
			}
		});
		SIZE_LIMIT = (int) (FRAME_LIMIT*0.25);
		executor = Executors.newFixedThreadPool(4);
		loadGIFs = this.startPlaying();
	}

	public Future<Boolean> startPlaying(){
		return executor.submit(() -> {
			try {
				String link = "https://api.giphy.com/v1/gifs/trending?api_key=4hPhmDbivx1So3XoQ5hJFnDLRhJq9Wg9&limit=20&rating=G";
				JSONObject completeReponse = getGIFS(link);
				JSONArray gifs = completeReponse.getJSONArray("data");

				gifs.forEach(gif->{
					try {
						executor.submit(
								new DissecateGIF(writeGIF(
										((JSONObject) gif).getJSONObject("images").getJSONObject("original").getString("url"), 
										((JSONObject) gif).getString("id"))));
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			return true;
		});
		
	}

	public synchronized void appendGIF(ArrayList<ImageIcon> gif){
		for(ImageIcon gifFrame : gif){
			try {
				roller.put(gifFrame);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		GenevaGIF jg = new GenevaGIF();
	}

	public JSONObject getGIFS(String urlToRead) throws Exception {
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlToRead);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		rd.close();
		final JSONObject gifs = new JSONObject(result.toString());
		return gifs;
	}

	public String writeGIF(String gifURL, String name) throws Exception {
		String gifFilePath = "C:\\Users\\Luis\\Desktop\\"+name+".gif";
		System.out.println("Enter Write GIF");
		System.out.println("URL: "+gifURL);
		
		//Change URL to get raw gif file
		Pattern p = Pattern.compile("(https://)(media[0-9])(.+)");
		Matcher m = p.matcher(gifURL);
		StringBuffer sb = new StringBuffer(gifURL.length());
		while(m.find()){
			sb.append(m.group(1));
			sb.append("i");
			sb.append(m.group(3));
		};
		
		//IF the GIF already exists only return path to file
		File file = new File(gifFilePath);
		if(file.exists()) { 
		    return gifFilePath;
		}
		
		//Retrive the GIF file from HTTP and write to a file
		URL url = new URL(sb.toString());
		InputStream is = url.openStream();
		file.getParentFile().mkdirs();
		file.createNewFile();
		OutputStream os = new FileOutputStream(gifFilePath);
		byte[] b = new byte[2048];
		int length;

		while ((length = is.read(b)) != -1) {
			os.write(b, 0, length);
		}
		is.close();
		os.close();
		
		//Return path to file
		return gifFilePath;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		switch( keyCode ) { 
		case KeyEvent.VK_UP:
			try {
				System.out.println("Number of frames: "+roller.size());
				if(roller.size() > 0){
					ImageIcon lFrame;
					lFrame = roller.take();
					label.setIcon(lFrame);
					label.repaint();
					repaint();
					System.out.println("SIZE LIMIT: "+SIZE_LIMIT);
					if(loadGIFs.isDone() && roller.size()<=SIZE_LIMIT){
						loadGIFs = this.startPlaying();
					}
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			break;
		case KeyEvent.VK_DOWN:
			// handle down 
			break;
		case KeyEvent.VK_LEFT:
			// handle left
			break;
		case KeyEvent.VK_RIGHT :
			try {
				System.out.println("Number of rames: "+roller.size());
				if(roller.size() > 0){
					ImageIcon lFrame;
					lFrame = roller.take();
					label.setIcon(lFrame);
					label.repaint();
					repaint();
					if(loadGIFs.isDone() && roller.size()<=SIZE_LIMIT){
						loadGIFs = this.startPlaying();
					}				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	private class DissecateGIF implements Runnable{

		private String gifPath;
		private ArrayList<ImageIcon> frames;

		public DissecateGIF(String path){
			this.gifPath = path;
			this.frames = new ArrayList<ImageIcon>();
		}

		@Override
		public void run() {
			dissecateGIF(gifPath);
		}

		public void dissecateGIF(String gif){
			try{
				BufferedImage master = null;
				String[] imageatt = new String[]{
						"imageLeftPosition",
						"imageTopPosition",
						"imageWidth",
						"imageHeight"
				};

				ImageReader reader = (ImageReader)ImageIO.getImageReadersByFormatName("gif").next();
				ImageInputStream ciis = ImageIO.createImageInputStream(new File(gif));
				reader.setInput(ciis, false);
				int noi = reader.getNumImages(true);

				for (int i = 0; i < noi; i++) {
					BufferedImage slave = null;
					BufferedImage image = reader.read(i);
					IIOMetadata metadata = reader.getImageMetadata(i);

					IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
					NodeList children = tree.getChildNodes();

					for (int j = 0; j < children.getLength(); j++) {
						Node nodeItem = children.item(j);

						if(nodeItem.getNodeName().equals("ImageDescriptor")){
							Map<String, Integer> imageAttr = new HashMap<String, Integer>();

							for (int k = 0; k < imageatt.length; k++) {
								NamedNodeMap attr = nodeItem.getAttributes();
								Node attnode = attr.getNamedItem(imageatt[k]);
								imageAttr.put(imageatt[k], Integer.valueOf(attnode.getNodeValue()));
							}
							if(i==0){
								master = new BufferedImage(imageAttr.get("imageWidth"), imageAttr.get("imageHeight"), BufferedImage.TYPE_INT_ARGB);
							}
							master.getGraphics().drawImage(image, imageAttr.get("imageLeftPosition"), imageAttr.get("imageTopPosition"), null);


							slave = new BufferedImage(imageAttr.get("imageWidth"), imageAttr.get("imageHeight"), BufferedImage.TYPE_INT_ARGB);
							slave.getGraphics().drawImage(master, imageAttr.get("imageLeftPosition"), imageAttr.get("imageTopPosition"), null);
						}
					}
					ImageIcon icon = new ImageIcon(slave);
					frames.add(icon);

					//			        label.setIcon(icon);
					//			        Thread.currentThread().sleep(100);
					//ImageIO.write(master, "GIF", new File("C:\\Users\\Luis\\Desktop\\"+ i + ".gif")); 
				}

				appendGIF(frames);

			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}
