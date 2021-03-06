package com.example.testapp;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
// Imports api GraphView
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;
// Imports api ThinkGear
import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;
import com.neurosky.thinkgear.TGRawMulti;
/**
 * Permet l'affichage du graphique avec les diff�rentes donn�es recueillies en temps r�el :
 * - Attention
 * - M�ditation
 * - Clins d'oeil
 * @author Robin, Chafik, Lo�c, C�cile
 *
 */

public class MainActivity extends Activity {

	//D�claration des variables
	TGDevice tgDevice;
	BluetoothAdapter btAdapter;
	
	// Echelle Y du graph pour Meditation et Attention
    double max = 100 ;
    double min = 0;
    
    int passage = 1;
    
    //variables par d�faut du menu de param�tres
    boolean courbeAttention = true;
    boolean courbeMeditation = true;
    boolean courbeBlink = true;
    boolean valuesRecord = false;
    int timeRecord = 30;
    
    //getter de valeurs
    boolean getAttention = false;
    boolean getMeditation = false;
    boolean getBlink = false;
    boolean getRawData = false;
    ArrayList<Integer> meditationValues = new ArrayList<Integer>();
    ArrayList<Integer> attentionValues = new ArrayList<Integer>();
	
    // Courbe de l'attention (Couleur = rouge / Nom = Attention)
    GraphViewSeries seriesAttention = new GraphViewSeries("Attention", new GraphViewSeriesStyle(Color.rgb(200, 50, 00), 3), new GraphViewData[] {

      });
    // Courbe de la m�ditation (Couleur = bleu / Nom = Meditation)
    GraphViewSeries seriesMeditation = new GraphViewSeries("Meditation", new GraphViewSeriesStyle(Color.rgb(0, 50, 200), 3), new GraphViewData[] {

    });
    
    // Courbe des clins d'oeil (Couleur = vert / Nom = clins d'oeil)
    GraphViewSeries seriesBlink = new GraphViewSeries("Clins d'oeil", new GraphViewSeriesStyle(Color.rgb(0, 200, 50), 3), new GraphViewData[] {

    });
    
    // Instanciation du GraphView
    GraphView graphView;
	
    /**
	 * Permet de trouver les listes des devices appair�s et ceux dans la port�e du bluetooth
	 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter != null) 
        { 
        	tgDevice = new TGDevice(btAdapter, handler); 
        }
        
        
        tgDevice.connect(true);
        createGraph();
	}
    
    /**
     * Herite de la methode onResume() et lance la m�thode gestionParametre()
     */
    protected void onResume(){
    	super.onResume();
    	
    	gestionParametre();
    }
    
    /**
     * Permet de g�rer les param�tres
     */
    protected void gestionParametre(){
    	try{
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	    	
	    	boolean oldCourbeAttention = courbeAttention;
	    	boolean oldCourbeMeditation = courbeMeditation;
	    	boolean oldCourbeBlink = courbeBlink;
	    	boolean oldValuesRecord = valuesRecord;
	    	int oldTimeRecord = timeRecord;
	    	courbeAttention = prefs.getBoolean("graph_attention", true);
	    	courbeMeditation = prefs.getBoolean("graph_meditation", true);
	    	courbeBlink = prefs.getBoolean("graph_blink", true);
	    	valuesRecord = prefs.getBoolean("values_record", false);
	    	timeRecord = Integer.parseInt(prefs.getString("time_record", "30"));
	    	
    	
	    	if(!courbeAttention){
	    		graphView.removeSeries(seriesAttention);
	    	}else if(courbeAttention != oldCourbeAttention){
	    		graphView.addSeries(seriesAttention);
	    	}
	    	
	    	if(!courbeMeditation){
	    		graphView.removeSeries(seriesMeditation);
	    	}else if(courbeMeditation != oldCourbeMeditation){
	    		graphView.addSeries(seriesMeditation);
	    	}
	    	
	    	if(!courbeBlink){
	    		graphView.removeSeries(seriesBlink);
	    	}else if(courbeBlink != oldCourbeBlink){
	    		graphView.addSeries(seriesBlink);
	    	}
	    	
	    	if(!courbeAttention && !courbeBlink && !courbeMeditation){
	    		Toast.makeText(getApplicationContext(), "Aucune courbe s�lectionn�, s�lectionnez en une dans les param�tres de l\'application", Toast.LENGTH_LONG).show();
	    	}
	    	Log.v("MsgRecordParam", "ValuesRecord : "+valuesRecord);
	    	if(valuesRecord){
	    		startRecord();
	    	}else if(!valuesRecord && oldValuesRecord){
	    		
	    	}
    	}catch(Exception exc){
    		Log.e("errorResume", "Erreur : " +exc);
    	}
    }
    
    /**
     * Permet d'enregistrer les donn�es recueillies dans un laps de temps
     */
    private void startRecord() {
		
    	Log.v("MsgRecordStart", "Passage");
    	getAttention = true;
    	getMeditation = true;
    	
    	Timer timer = new Timer();
    	
    	timer.schedule(new TimerTask() {
    		  @Override
    		  public void run() {
    			  Log.v("MsgRecordRun", "Passage run");
    			  Log.v("MsgRecordRun", "ALM : "+meditationValues);
    		    //Code pour insertion dans le csv puis arret
    			  valuesRecord = false;
    			  getAttention = false;
    			  getMeditation = false;
    			  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    			  SharedPreferences.Editor editor = prefs.edit();
    			  editor.putBoolean("values_record", false);
    			  editor.commit();
    			  Log.v("MsgRecordRun", "test1");
    			  ArrayList<String> entete = new ArrayList<String>();
    			  entete.add("Attention");
    			  entete.add(",");
    			  entete.add("Meditation");
    			  entete.add("\n");
    			  Log.v("MsgRecordRun", "test2");
    			  csvWriter csvFile = new csvWriter("recordFile"+ (int)Math.random()*(9999999-0000000)+1 +".csv");
    			  Log.v("MsgRecordRun", "test5");
    			  csvFile.addCSVTwoList(meditationValues, attentionValues, entete);
    			  Log.v("MsgRecordRun", "test6");
    		  }
    		}, timeRecord*1000);
		
	}
    
    /**
     * Handler du ThinkGear Device (thread qui traite constamment les donn�es re�us)
     */
    @SuppressLint("HandlerLeak") private final Handler handler = new Handler() {
    	@SuppressWarnings("deprecation")
		@Override
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
    			case TGDevice.MSG_STATE_CHANGE:
    				switch (msg.arg1) {
    					case TGDevice.STATE_IDLE:
    					break;
    					case TGDevice.STATE_CONNECTING:
    						Log.v("Statut", "Connection en cours ...");
    						Toast.makeText(getApplicationContext(), "Connection en cours ...", Toast.LENGTH_SHORT).show();
						break;
						case TGDevice.STATE_CONNECTED:
							Log.v("Statut", "Connect�");
							Toast.makeText(getApplicationContext(), "Connect� !", Toast.LENGTH_SHORT).show();
							tgDevice.start();
						break;
						case TGDevice.STATE_DISCONNECTED:
							Toast.makeText(getApplicationContext(), "Systeme d�connect� !", Toast.LENGTH_SHORT).show();
						break;
						case TGDevice.STATE_NOT_FOUND:
							Toast.makeText(getApplicationContext(), "Systeme non trouv� !", Toast.LENGTH_SHORT).show();
							finish(); //Nous renvoie sur le menu
						break;
						case TGDevice.STATE_NOT_PAIRED:
							Log.v("Statut", "Not paired");
							tgDevice.connect(true);
						default:
						break;
    				}
    			break;
    			case TGDevice.MSG_POOR_SIGNAL: // Indique la qualit� du signal (0 indique un bon fonctionnement)
    				Log.v("MsgEEG", "PoorSignal: " + msg.arg1);
    			break;
    			case TGDevice.MSG_ATTENTION:
    				/*
    				 * Renvoie une valeur entre 0 et 100 sur la capacit� d'attention
    				 * 0 : incapacit� � calcul� une valeur d'attention
    				 * 1 - 20 : tres faible attention
    				 * 20 - 40 : faible attention
    				 * 40 - 60 : valeur d'attention normal
    				 * 60 - 80 : attention relativement �lev�
    				 * 80 - 100 : attention �lev�
    				 */
    				Log.v("MsgEEG", "Attention: " + msg.arg1);
    				  seriesAttention.appendData( new GraphViewData(passage, msg.arg1), true);
    				  if(getAttention){
    					  attentionValues.add(msg.arg1);
    				  }
    				  passage++;
    			break;
    			case TGDevice.MSG_MEDITATION:
    				/*
    				 * Renvoie une valeur entre 0 et 100 sur la capacit� de m�ditation
    				 * 0 : incapacit� � calcul� une valeur de meditation
    				 * 1 - 20 : tres faible meditation
    				 * 20 - 40 : faible meditation
    				 * 40 - 60 : valeur de meditation normal
    				 * 60 - 80 : meditation relativement �lev�
    				 * 80 - 100 : meditation �lev�
    				 */
    				Log.v("MsgEEG","Meditation: " +msg.arg1);
    				seriesMeditation.appendData( new GraphViewData(passage, msg.arg1), true);
  				    if(getMeditation){
					    meditationValues.add(msg.arg1);
				    }
    			break;
    			case TGDevice.MSG_RAW_DATA:
    				int rawValue = msg.arg1;
    				int test = msg.arg2;
    				Log.v("MsgRawData", "Raw Data : " +rawValue + " / " +test);
    			break;
    			case TGDevice.MSG_HEART_RATE:
    				Log.v("MsgEEG","Heart Rate " +msg.arg1);
    			break;
    			case TGDevice.MSG_BLINK:
    				seriesBlink.appendData( new GraphViewData(passage, msg.arg1), true);
    				Log.v("MsgEEG","Blink : " +msg.arg1);
    			break;
    			case TGDevice.MSG_SLEEP_STAGE:
    				Log.v("MsgEEG", "Sleep stage : " +msg.arg1);
    			break;
    			case TGDevice.MSG_RAW_MULTI:
                	TGRawMulti rawM = (TGRawMulti)msg.obj;
                	Log.v("MsgRawMulti","Raw1: " + rawM.ch1 + "\nRaw2: " + rawM.ch2);
    			break;
                case TGDevice.MSG_LOW_BATTERY:
                	Toast.makeText(getApplicationContext(), "Batterie faible !", Toast.LENGTH_SHORT).show();
                break;
    			case TGDevice.MSG_EEG_POWER:
    				TGEegPower ep = (TGEegPower)msg.obj;
    				Log.v("MsgEEGD", "Delta: " + ep.delta);
    				Log.v("MsgEEGGL","Gamma Low : " + ep.lowGamma);
    				Log.v("MsgEEGGM","Gamma Mid : " + ep.midGamma);
    			default:
    			break;
    		}
    	}
    };

    /**
     * Cr�ation du graphique attention / meditation
     */
    public void createGraph(){

    	graphView = new LineGraphView(this, "Courbes EEG");
    	
    	graphView.addSeries(seriesMeditation);
    	graphView.addSeries(seriesAttention);
    	graphView.addSeries(seriesBlink);
    	
		graphView.setManualYAxisBounds((double) max, (double) min);
		graphView.setShowLegend(true);
		graphView.setViewPort(1,25);
		graphView.setScrollable(true);
		graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.WHITE);
		graphView.getGraphViewStyle().setLegendWidth(200);
		graphView.setLegendAlign(LegendAlign.TOP);
		graphView.getGraphViewStyle().setNumVerticalLabels(5);
		graphView.getGraphViewStyle().setNumHorizontalLabels(25);
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout1);
		layout.addView(graphView);
    }

    /**
     * Ajoute des objets dans la barre d'action
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    /**
     * Ajoute des objets dans la barre d'action :
     * - Param�tres : lance un nouvel intent de SettingsActivity
     * - A propos : lance une bo�te de dialogue avec les noms des d�veloppeurs de l'application
     * - Aide : lance un nouvel intent avec une page de memo
     * - quitter
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.settings:
    		// Comportement du bouton "Param�tres"
    		Intent settingsIntent = new Intent(this,SettingsActivity.class);
    		startActivity(settingsIntent);
    		return true;
    	case R.id.propos:
    		// Comportement du bouton "A propos"
    		new AlertDialog.Builder(this)
    	    .setTitle("A propos")
    	    .setMessage("Application r�alis� dans le cadre du projet BrainWaves de Licence Pro Dev Web et Mobile d'Orleans.\n" +
    	    		"- Robin Hayart \n" +
    	    		"- Loic Dieudonn� \n" +
    	    		"- Chafik Daggag \n" +
    	    		"- Cecile Kergall")
    	    .setIcon(android.R.drawable.ic_dialog_alert)
    	     .show();
    		return true;
    	case R.id.aide:
    		// Comportement du bouton "Aide"
    		Intent aideIntent = new Intent(this,AideActivity.class);
    		startActivity(aideIntent);
    		return true;
    	case R.id.quitter:
    		// Comportement du bouton "Quitter"
    		finish();
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
}
