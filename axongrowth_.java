/* *
	Copyright (C) 2014 Abel Torres Espín

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import ij.*;
import ij.macro.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.Image.*;
import ij.plugin.frame.*;
import ij.process.*;
import java.io.*;
import java.lang.*;
import ij.plugin.*;
import ij.measure.*;

public class axongrowth_ extends PlugInFrame implements ActionListener, Measurements{ 
	
	private Button btnrun, btnclose, btnscale, btnreset, btnback, btnnext, btnrefilter;
	private MultiLineLabel wmessage;
	private Label Tmessage, Tscale,Tdistancescale;
	private Checkbox mArea, mNeurite, mBody, nNeurite;
	private Panel infopanel;
	private TextField TFdistance, TFnoise;
	private static PlugInFrame instance;
	private ImagePlus imp, imp2, imp3, impOriginal, impNoise;
	private ImageProcessor ip, ip2;
	private ImageWindow iw;
	private RoiManager rm;
	private Frame RM;
	private ImageCalculator ic;
	private Calibration C;
	private Double scale, zoom2, min, min2, totalArea ;
	private ResultsTable rt, rpt;
	private Inforeport infomessage= new Inforeport();
	private String title;
	private Scrollbar sbNoise;
	private Point p;

	public axongrowth_() {
		super("Neurite-J 1.1");
		Boolean version=IJ.versionLessThan("1.49");
		if (instance!=null) {
           			instance.toFront();
            		return;
		}
		if (version==true){
			instance = null;
			close();
		}else{
			WindowManager.addWindow(this);
        			instance = this;
			createGUI();
			setVisible(true);
		}
	}

	public void createGUI(){
	//Principal frame settings
		setSize(220, 520);
		Rectangle bounds=getBounds();
		setLocation (1000+bounds.x,100+bounds.y);
		setLayout(null);
         		setForeground(Color.darkGray);
		setBackground(new Color(230,230,230));
          		setResizable(false);
          		setFont(new Font("Helvetica", Font.PLAIN, 14)); 
	 //Buttons	set buttons on the main ctrl board 
            	btnrun = new Button(" Run ");
            	btnrun.addActionListener(this);
		btnrun.setFont(new Font("Helvetica", Font.BOLD, 18));
        		btnrun.setBounds(52, 340, 120, 60);
            	add(btnrun);
            	
		btnclose = new Button(" Close ");
            	btnclose.addActionListener(this);
            	btnclose.setFont(new Font("Helvetica", Font.PLAIN, 14));
        		btnclose.setBounds(120, 480, 65, 30);
		add(btnclose);

		btnscale = new Button(" Set scale ");
            	btnscale.addActionListener(this);
            	btnscale.setFont(new Font("Helvetica", Font.PLAIN, 14));
        		btnscale.setBounds(130, 243, 70, 30);
		add(btnscale);
		
		btnreset = new Button(" Restart ");
            	btnreset.addActionListener(this);
            	btnreset.setFont(new Font("Helvetica", Font.PLAIN, 14));
        		btnreset.setBounds(40, 480, 65, 30);
		btnreset.setEnabled(false);
		add(btnreset);

		btnback= new Button(" < Back ");
		btnback.addActionListener(this);
            	btnback.setFont(new Font("Helvetica", Font.PLAIN, 14));
        		btnback.setBounds(40, 436, 65, 25);
		btnback.setEnabled(false);
		add(btnback);
		
		btnnext= new Button(" Next > ");
		btnnext.addActionListener(this);
            	btnnext.setFont(new Font("Helvetica", Font.PLAIN, 14));
        		btnnext.setBounds(120, 436, 65, 25);
		btnnext.setEnabled(false);
		add(btnnext);
		
		sbNoise = new Scrollbar(Scrollbar.HORIZONTAL, 100, 20, 0, 1000);
		sbNoise.setBounds(70,395,135,22);
		sbNoise.addAdjustmentListener(new AdjustmentListener() {
         			public void adjustmentValueChanged(AdjustmentEvent e) {
            			TFnoise.setText(""+sbNoise.getValue()+"");
				if (!e.getValueIsAdjusting()){
					NoiseFilter(sbNoise.getValue());	
				}
			}
		});
		sbNoise.setVisible(false);
 		add(sbNoise);

	// Checkbox options
		mArea= new Checkbox(" Neurite occupied area", true);
		mArea.setLocation(30,142);
		mArea.setSize(170,20);
		add(mArea);
		
		nNeurite= new Checkbox(" Number of neurites", false);
		nNeurite.setLocation(30,164);
		nNeurite.setSize(170,20);
		add(nNeurite);
		
		mNeurite= new Checkbox(" Neurite intersection", true);
		mNeurite.setLocation(30,120);
		mNeurite.setSize(170,20);
		add(mNeurite);
		
		mBody= new Checkbox(" Explant body stats", true);
		mBody.setLocation(30,186);
		mBody.setSize(170,20);
		add(mBody);
	//Texts	
		TFdistance = new TextField("25");
		Label TCdistance= new Label("Distance: ");
		Tdistancescale=new Label("units");
		Tdistancescale.setBounds(145,65,40,20);
		TFdistance.setEditable(true);
             	TFdistance.setBounds(100, 64, 40, 21);
		TCdistance.setBounds(25,65,80,20);
		TCdistance.setBackground(new Color(230,230,230));
		TCdistance.setFont(new Font("Helvetica", Font.PLAIN, 14));
         		add(TFdistance);
		add(TCdistance);
		add(Tdistancescale);

		TFnoise = new TextField(""+sbNoise.getValue()+"");
		TFnoise.setEditable(true);
             	TFnoise.setBounds(15, 395, 50, 22);
		TFnoise.addTextListener (new TextListener(){
			public void textValueChanged(TextEvent e) {
         			sbNoise.setValue(Integer.parseInt(TFnoise.getText())); 
			NoiseFilter(sbNoise.getValue());	              
      			}
		});
		TFnoise.setVisible(false);
         		add(TFnoise);
		
		Label measures=new Label ("Measures: ");
		measures.setBounds(10,90,70,28);
		measures.setFont(new Font("Helvetica", Font.PLAIN, 14));
		add(measures);
		
		Label scale=new Label ("Scale: ");
		scale.setBounds(5,220,50,20);
		Tscale=new Label("   <no scale>");
		Tscale.setBounds(15,247,150,20);
		scale.setFont(new Font("Helvetica", Font.BOLD, 14));
		add(Tscale);
		add(scale);

	// Informative panels that tell the progess of the analysis
		Label status =new Label("Status:");
		status.setBounds(5,270,200,30);
		status.setFont(new Font("Helvetica", Font.BOLD, 14));
		add(status);

		Label opt =new Label("Options:");
		opt.setBounds(5,25,200,25);
		opt.setFont(new Font("Helvetica", Font.BOLD, 14));
		add(opt);
		
		infopanel=new Panel();
		infopanel.setBounds (7,301,187,130);
		infopanel.setLayout(null);
		Tmessage = new Label ("");
		Tmessage.setBounds(10, 2, 180,30);
		Tmessage.setFont(new Font("Helvetica", Font.BOLD, 16));
		wmessage= new MultiLineLabel ("");
		wmessage.setFont(new Font("Helvetica", Font.PLAIN, 16));
		wmessage.setBounds (15,25, 200, 100);
		infopanel.add(Tmessage);
		infopanel.add(wmessage);
		add(infopanel);
		
	}
	public void paint(Graphics g){
		g.drawRect(6,300,206,170);
		g.drawRect(6,50,206,165);
	}
	
	public void pInicio(){
		Tmessage.setText("");
		wmessage.setText("");
		btnrun.setVisible(true);
		btnnext.setEnabled(false);
		btnback.setEnabled(false);
		btnreset.setEnabled(true);
		sbNoise.setVisible(false);
		TFnoise.setVisible(false);
	}
	
	public void restart(){
		impOriginal.setTitle(title);
		iw.setImage(impOriginal);
		IJ.run("Set... ", "zoom="+zoom2+" x=1478 y=955");
		IJ.run("Original Scale", "");
		IJ.run("Remove Overlay", "");
		RM=WindowManager.getFrame("ROI Manager");
		if (RM instanceof PlugInFrame) {
			((PlugInFrame)RM).close();
		}
	}
	public void GetImage(){
		imp=WindowManager.getCurrentImage();	
		if (imp==null){
			IJ.noImage();
		}
		iw=imp.getWindow();
		
		title=imp.getTitle();
		impOriginal=imp.flatten();
		zoom2 =iw.getInitialMagnification();
		if (imp!=null){
			C=imp.getCalibration();
			scale=Math.round(1/C.getX(1)*Math.pow(10,2))/Math.pow(10,2);
			Tscale.setText(scale+" pixels"+"/"+C.getUnits());
			Tdistancescale.setText(C.getUnits());
		}
	}
	public void runbutton(){
		RM=WindowManager.getFrame("ROI Manager");
		if (RM instanceof PlugInFrame) {
			((PlugInFrame)RM).close();
		}
		IJ.setTool("Freehand Selections");

		GetImage();
		rm=new RoiManager();		

		IJ.run(imp, "Unsharp Mask...", "radius=1 mask=0.60");
		IJ.run("8-bit");
		IJ.run(imp, "Select None", "");
		IJ.run("Remove Overlay", "");
		
		IJ.run("Colors...", "foreground=white background=black selection=green");
		IJ.run("Set Measurements...", "area limit display redirect=None decimal=3");
						
		int h=imp.getHeight();
		int w=imp.getWidth();
		IJ.doWand(imp, 0, 0, 0.0, "Legacy");
		rm.addRoi(imp.getRoi());
		IJ.doWand(imp, w-1, 0, 0.0, "Legacy");
		rm.addRoi(imp.getRoi());
		IJ.doWand(imp, 0, h-1, 0.0, "Legacy");
		rm.addRoi(imp.getRoi());
		IJ.doWand(imp, w-1, h-1, 0.0, "Legacy");
		rm.addRoi(imp.getRoi());
		IJ.run(imp, "Select None", "");

		IJ.run("Threshold...");
		btnrun.setVisible(false);
		btnreset.setEnabled(true);
		btnnext.setEnabled(true);
		
		infomessage.selectbody();
	}

	public void bodyselection(){
		ip=imp.getProcessor();
                	double min1=ip.getMinThreshold();
		ip.setThreshold(min1, 254, 0);//ip becomes the explant body only
		imp.updateAndDraw();
		IJ.run("Analyze Particles...", "size=10000-Infinity circularity=0.0-1.00 show=Nothing add");
		IJ.run("Remove Overlay", "");
		rm.runCommand("Show None");
		
		if (rm.getCount()==0){
			IJ.showMessage("Selection error", "Neurite-J could not find a selection. Please check the scale");
		}else{
			rm.select(imp,4);
			IJ.run(imp, "Enlarge...", "enlarge="+(-50));
			IJ.run(imp, "Enlarge...", "enlarge="+50);
			IJ.run(imp, "Interpolate", "interval=1");
			rm.runCommand("Update");
		}

		infomessage.correctbody();
		IJ.setTool("brush");
	}

	public void background(){
		rm.runCommand("Update");
		infomessage.noise();
		IJ.run(imp, "Select None", "");
		imp2=imp.duplicate();
		ip2=imp2.getProcessor();
				
		IJ.run(imp,"Subtract Background...", "rolling=50 sliding");

		IJ.setForegroundColor(0, 0, 0);
		for(int i=0;i<4;i++){
			rm.select(imp, 0);
			IJ.run(imp, "Fill", "slice");
			rm.runCommand("Delete");
		}

		infomessage.selectionneurite();
		IJ.run("Threshold...");
	}
	
	public void neuriteselect(){
		String dnoise=TFnoise.getText();
		int noise= Integer.parseInt(dnoise);//the number chosen from the scrollbar

		min=ip.getMinThreshold();
		ip.setThreshold(min, 255, 0);//get the min of the explant body then substract this as the threshold
		imp2=imp.duplicate();
		IJ.run(imp, "Make Binary", "thresholded remaining black");//2 lines are tgt imp is now the binary. imp2 is the original	
		imp3=imp.duplicate();
		IJ.run(imp, "Analyze Particles...", "size="+noise+"-infinity circularity=0.00-1.00 show=Masks in_situ");
		infomessage.selectnoise();
	}
	
	public void NoiseFilter(int noise){
		imp.setImage(imp3);
		IJ.run(imp, "Analyze Particles...", "size="+noise+"-infinity circularity=0.75-1.00 show=Masks in_situ");
	}
	
	public void manualnoise(){
		IJ.run(imp, "Smooth", "");
		IJ.run(imp, "Make Binary", "");
		IJ.setForegroundColor(255, 255, 255);
		IJ.setTool("Paintbrush Tool");
		infomessage.manualnoise();
	}
	
	public void measure(){
		//IJ.run("Clear Results");//should delete this for later 2 tables
		rt=ResultsTable.getResultsTable();
		rpt = new ResultsTable();
		wmessage.setVisible(false);
		Tmessage.setBounds(10, 52, 180,30);
		btnnext.setEnabled(false);
		btnback.setEnabled(false);
		String dstring=TFdistance.getText();//distance between the 2 concentric rings
		int d= Integer.parseInt(dstring);//get the numeric value of the distance you choose
		int m=d;
		int count=0;
		
		ImageStatistics is;
		
		C=imp.getCalibration();
		ip=imp.getProcessor();
		
		double[] distance = new double[(int)(ip.getHeight()/d)+1];//onset number of distances to be measured
		double[] intersection = new double[(int)(ip.getHeight()/d)+1];//onset number of intersections
		double[] vArea = new double[(int)(ip.getHeight()/d)+1];//onset number of area to be measured, number of area to be measured is (ip.getHeight()/d)+1
		int[] indexes=new int[2];
		int[] valuepixel;
		Roi selection;
		Polygon p;
		int value;
		
		for(int i=0; i<m+d*2;i+=d){
			Tmessage.setText("Measuring: "+i+" "+Tdistancescale.getText());
			rm.select(imp, i/d);
			if(mArea.getState()==true){
				is=imp.getStatistics(AREA+ LIMIT);
				vArea[i/d]=is.area;
			}
			distance[i/d]=(i/d)*d;//specific distance of the point i in the array
			indexes[0]=i/d;//first point of each distance
			indexes[1]=i/d+1;//second point of each distance
			if(mNeurite.getState()==true){
				selection =imp.getRoi();
				p=selection.getPolygon();
				valuepixel=new int[p.npoints];
				for (int j=0; j<p.npoints; j++){
					value=ip.getPixel(p.xpoints[j], p.ypoints[j]);
					valuepixel[j]=value;
				}
				count=0;
				for (int t=0; t<p.npoints; t++){
     					if (t!=0){
	 					if (valuepixel[t]<=100 && valuepixel[t-1]>=100 ){
							count=count+1;
						}
    					}
				}
			}
			intersection[i/d]=count;
			rm.select(imp, i/d);
			IJ.run("Enlarge...", "enlarge="+d);
			IJ.run(imp, "Interpolate", "interval=1");
			rm.runCommand("add");
			
			if(mNeurite.getState()==true&&mArea.getState()==true){
				if(i!=0) 
					if(intersection[i/d]!=0) m=m+d;
			}
			if(mNeurite.getState()==true&&mArea.getState()==false){
				if(i!=0) 
					if(intersection[i/d]!=0) m=m+d;
			}
			if(mNeurite.getState()==false&&mArea.getState()==true){
				if(i!=0) 
					if(vArea[i/d]!=vArea[i/d-1]) m=m+d;
			}
		}
				
		double[] fArea = new double[rm.getCount()+1];
		for(int i=0;i<rm.getCount()+1;i+=1){
			fArea[i]=vArea[i+1]-vArea[i];
			if (fArea[i]<0) fArea[i]=0;
			}
		
		totalArea = 0.0;
		for (double measure:fArea) {
			totalArea = measure+totalArea;
		}
		rpt.incrementCounter();
		rpt.addValue("Image Title", title);
		rpt.addValue("Axon growth area ("+Tdistancescale.getText()+")", totalArea);
				
		for(int i=0;i<rm.getCount()-1;i+=1){
			if(mNeurite.getState()==true&&mArea.getState()==true){
				rt.setValue("Distance "+ "("+Tdistancescale.getText()+")", i, (int)distance[i]);
				rt.setValue("Intersections",i,(int)intersection[i]);
				rt.setValue("Distance Area "+ "("+Tdistancescale.getText()+")", i, (int)distance[i]+"-"+(int)distance[i+1]);
				rt.setValue("Area",i,fArea[i]);
			}
			if(mNeurite.getState()==true&&mArea.getState()==false){
				rt.setValue("Distance "+ "("+Tdistancescale.getText()+")", i, (int)distance[i]);
				rt.setValue("Intersections",i,(int)intersection[i]);
			}
			if(mNeurite.getState()==false&&mArea.getState()==true){
				rt.setValue("Distance Area"+ "("+Tdistancescale.getText()+")", i, (int)distance[i]+"-"+(int)distance[i+1]);
				rt.setValue("Area",i,fArea[i]);
			}
		}
		
		
		
		if(mBody.getState()==true){
			rm.select(imp, 0);
			selection =imp.getRoi();
			is=imp.getStatistics(AREA);
			double bodyArea=is.area;
			double perimeter = selection.getLength();//need to add width and height
			double bodyCir= perimeter==0.0?0.0:4.0*Math.PI*(is.area/(perimeter*perimeter));
			rt.setValue("Body Stat",0,"Area" + "("+Tdistancescale.getText()+"^2"+")");
			rt.setValue("Body Value",0,bodyArea);
			rt.setValue("Body Stat",1,"Circularity");
			rt.setValue("Body Value",1,bodyCir);
			for (int i=2; i<rm.getCount()-1;i++){
					rt.setValue("Body Stat",i,"");
					rt.setValue("Body Value",i,"");
				}
			rpt.addValue("Explant area", (double) bodyArea);
			rpt.addValue("Circularity", (double) bodyCir);
		}
		
		if(nNeurite.getState()==true) {
			double AxonNumber = intersection[1];
			rpt.addValue("Outgrowth Axon number", (double) AxonNumber);
		}
		
		rt.show("Results");
		//IJ.selectWindow("Results");
		rpt.show("Neurite arbor");
		IJ.selectWindow("Neurite arbor");//choosing the window to pop up
		rm.runCommand("Show All");
		imp.updateAndDraw();
		pInicio();
	}	
	

	public void actionPerformed(ActionEvent e) {
            	Object b = e.getSource();
            	 if (b==btnrun) runbutton();
            	if (b==btnclose){
                 		instance=null;
			close();
            	}
		if (b==btnnext){
			if (Tmessage.getText()=="Body selection") bodyselection();	
			else if (Tmessage.getText()=="Body correction") background();
			else if 	(Tmessage.getText()=="Neurite selection")neuriteselect();
			else if  (Tmessage.getText()=="Noise Filtering") manualnoise();
			else if 	(Tmessage.getText()=="Manual noise") measure();
		}
		if (b==btnback){	
			if (Tmessage.getText()=="Body correction") {
				restart();
				pInicio();
			}
			else if (Tmessage.getText()=="Noise Filtering"){
				infomessage.selectionneurite();
				IJ.run("Threshold...");
				btnback.setEnabled(false);
				imp.setImage(imp2);
				ip=imp.getProcessor();
				IJ.setThreshold(imp, min, 255);
			}
			else if 	(Tmessage.getText()=="Manual noise"){
				infomessage.selectnoise();
				imp.setImage(imp3);
			}
		}
		if (b==btnscale){ 
			IJ.run("Set Scale...");
		}
		if (b==btnreset){ 
			pInicio();
			restart();
		}
 	}	
	
        public void windowClosing(WindowEvent event) { 
			instance=null;
			close();} 
        public void windowClosed(WindowEvent event)  {} 
        public void windowDeiconified(WindowEvent event){} 
        public void windowIconified(WindowEvent event){} 
        public void windowActivated(WindowEvent event){
			imp=WindowManager.getCurrentImage();
			C=imp.getCalibration();
			scale=Math.round(1/C.getX(1)*Math.pow(10,2))/Math.pow(10,2);
			Tscale.setText(scale+" pixels"+"/"+C.getUnits());
			Tdistancescale.setText(C.getUnits());
		} 
        public void windowDeactivated(WindowEvent event){} 
        public void windowOpened(WindowEvent event){} 
	
	class Inforeport{
		
		void selectbody(){ 
			Tmessage.setText("Body selection");
			Tmessage.setBounds(10, 2, 180,30);
			wmessage.setText("Unmask the explant \nbody using the threshold \nand click Next.");
			wmessage.setBounds (15,25, 200, 100);
			wmessage.setVisible(true);
		}
		void correctbody(){
			Tmessage.setText("Body correction");
			Tmessage.setBounds(10, 2, 180,30);
			wmessage.setText("Corrects the body ROI \nif necessary and \nclick Next.");
			wmessage.setBounds (15,25, 200, 100);
			wmessage.setVisible(true);
		}
		void noise(){
			Tmessage.setText("Background...");
			Tmessage.setBounds(10, 52, 180,30);
			wmessage.setVisible(false);
			btnnext.setEnabled(false);
			btnback.setEnabled(false);
		}
		void selectionneurite(){
			Tmessage.setText("Neurite selection");
			Tmessage.setBounds(10, 2, 180,30);
			wmessage.setText("Unmask the neurites \nusing the threshold \nand click Next.");
			wmessage.setBounds (15,25, 180, 100);
			wmessage.setVisible(true);
			sbNoise.setVisible(false);
			TFnoise.setVisible(false);
			btnnext.setEnabled(true);
		}
		void selectnoise(){
			Tmessage.setText("Noise Filtering");
			wmessage.setText("Select a filter value \nand click Next");
			wmessage.setBounds (25,10, 180, 100);
			sbNoise.setVisible(true);
			TFnoise.setVisible(true);
			btnback.setEnabled(true);
		}
		void manualnoise(){
			Tmessage.setText("Manual noise");
			Tmessage.setBounds(10, 2, 180,30);
			wmessage.setText("Clear the noise \n and click Next");
			wmessage.setBounds (35,25, 180, 100);
			sbNoise.setVisible(false);
			TFnoise.setVisible(false);
		}	
	}
}

