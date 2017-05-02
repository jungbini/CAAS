package caas;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.wb.swt.SWTResourceManager;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import it.polito.appeal.traci.*;

import caas.element.SimVehicle;
import caas.util.SASimUtil;

import org.eclipse.swt.browser.Browser;

public class SAConSimulator {
	
	// General variables
	public final String HOME_DIR = System.getProperty("user.home");
	

	public static boolean autoStepRunning = false;
	public double xPosWeight;
	public double yPosWeight;
	
	// SUMO related variables
	public static ArrayList<SimVehicle> simVehicles = new ArrayList<SimVehicle>();				// Current Sumo Vehicle List
	
	public static SumoTraciConnection connSumo;
	
	public String[] selType = new String[3];
	public String mapPath;
	
	// GUI related variables
	protected Shell shlSelfadaptiveContractSimulator;
	public Display display = Display.getDefault();
	public SelectMapDialog selMapDialog;
	public Browser browser;
	
	public Composite compMain;
	public TreeItem trtmNewTreeitem;
	public Tree NodeTree;
	private Table table;
	private Text txtLog;
	private Text txtEthDir;
	private Text txtRootDir;
	private Text txtDelay;
	private Text txtPeerDistance;
	private Label lblMapInfo;

	public static void main(String[] args) {	
				
		try {
			SAConSimulator window = new SAConSimulator();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void open() {
		display = Display.getDefault();
		
		createContents();
		
		browser = new Browser(shlSelfadaptiveContractSimulator, SWT.NONE);
		browser.setBounds(809, 567, 268, 231);
		
		selMapDialog = new SelectMapDialog(shlSelfadaptiveContractSimulator, SWT.BORDER | SWT.CLOSE | SWT.TITLE);
		selType = selMapDialog.open();
		makeSUMOMap(selType);										// Making sumo map before running simulation		
		
		shlSelfadaptiveContractSimulator.open();
		shlSelfadaptiveContractSimulator.layout();
		while (!shlSelfadaptiveContractSimulator.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	protected void createContents() {
		
		shlSelfadaptiveContractSimulator = new Shell(SWT.SHELL_TRIM & (~SWT.RESIZE));
		shlSelfadaptiveContractSimulator.setSize(1093, 840);
		shlSelfadaptiveContractSimulator.setText("Self-Adaptive Contract Simulator (test ver.)");
		shlSelfadaptiveContractSimulator.setLayout(null);
		
		Group grpSimulationSetting = new Group(shlSelfadaptiveContractSimulator, SWT.NONE);
		grpSimulationSetting.setText("Simulation Settings");
		grpSimulationSetting.setBounds(3, 7, 1074, 59);
		
		Label lblEthereumDir = new Label(grpSimulationSetting, SWT.NONE);
		lblEthereumDir.setBounds(229, 10, 97, 15);
		lblEthereumDir.setText("Ethereum Dir:");
		
		txtEthDir = new Text(grpSimulationSetting, SWT.BORDER);
		txtEthDir.setEnabled(false);
		txtEthDir.setText("ethereum");
		txtEthDir.setBounds(328, 6, 122, 23);	
		
		Label lblRootDir = new Label(grpSimulationSetting, SWT.NONE);
		lblRootDir.setText("Root Dir:");
		lblRootDir.setBounds(10, 10, 80, 15);
		
		txtRootDir = new Text(grpSimulationSetting, SWT.BORDER);
		txtRootDir.setEnabled(false);
		txtRootDir.setText("caas");
		txtRootDir.setBounds(73, 6, 122, 23);
		
		Label label = new Label(grpSimulationSetting, SWT.NONE);
		label.setText("Delay(ms):");
		label.setBounds(481, 10, 80, 15);
		
		txtDelay = new Text(grpSimulationSetting, SWT.BORDER);
		txtDelay.setText("500");
		txtDelay.setBounds(570, 6, 122, 23);
		
		Label lblPeerDistance = new Label(grpSimulationSetting, SWT.NONE);
		lblPeerDistance.setText("Peer Distance(m):");
		lblPeerDistance.setBounds(726, 10, 118, 15);
		
		txtPeerDistance = new Text(grpSimulationSetting, SWT.BORDER);
		txtPeerDistance.setText("100");
		txtPeerDistance.setBounds(858, 6, 122, 23);
		
		compMain = new Composite(shlSelfadaptiveContractSimulator, SWT.NONE);
		compMain.setBounds(3, 72, 800, 600);
		compMain.setBackground(SWTResourceManager.getColor(107, 142, 35));
		
		Button btnStartSim = new Button(compMain, SWT.NONE);
		btnStartSim.setBounds(670, 460, 120, 28);
		btnStartSim.setSelection(true);		
		btnStartSim.setFont(SWTResourceManager.getFont("Sans", 14, SWT.BOLD));
		btnStartSim.setText("Start");
		
		Button btnNextStep = new Button(compMain, SWT.NONE);
		btnNextStep.setBounds(670, 494, 120, 28);
		btnNextStep.setEnabled(false);		
		btnNextStep.setFont(SWTResourceManager.getFont("Sans", 14, SWT.NORMAL));
		btnNextStep.setText("Next Step");
		
		Button btnStopSim = new Button(compMain, SWT.NONE);
		btnStopSim.setBounds(670, 562, 120, 28);
		btnStopSim.setEnabled(false);
		btnStopSim.setFont(SWTResourceManager.getFont("Sans", 14, SWT.BOLD));
		btnStopSim.setText("Stop");
		
		Button btnAutoStep = new Button(compMain, SWT.NONE);
		
		btnAutoStep.setEnabled(false);
		btnAutoStep.setFont(SWTResourceManager.getFont("Sans", 14, SWT.NORMAL));
		btnAutoStep.setBounds(670, 528, 120, 28);
		btnAutoStep.setText("Auto Step");
		
		lblMapInfo = new Label(compMain, SWT.NONE);
		lblMapInfo.setBounds(10, 10, 370, 15);
		lblMapInfo.setText("Map information");
		
		Group grpNodeProperties = new Group(shlSelfadaptiveContractSimulator, SWT.NONE);
		grpNodeProperties.setText("Node & Properties");
		grpNodeProperties.setBounds(809, 72, 268, 489);
		
		table = new Table(grpNodeProperties, SWT.BORDER | SWT.FULL_SELECTION);
		table.setBounds(10, 299, 246, 162);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		settingTableItems();		
		
		TableColumn tblclmnNewColumn = new TableColumn(table, SWT.NONE);
		tblclmnNewColumn.setWidth(140);
		tblclmnNewColumn.setText("Property");
		
		TableColumn tblclmnNewColumn_1 = new TableColumn(table, SWT.NONE);
		tblclmnNewColumn_1.setWidth(100);
		tblclmnNewColumn_1.setText("Value");
		
		TreeViewer treeViewer = new TreeViewer(grpNodeProperties, SWT.BORDER);
		
		NodeTree = treeViewer.getTree();
		NodeTree.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {				
				TreeItem[] selection = NodeTree.getSelection();
				String nodeName = selection[0].getText(); 
				
				if (!nodeName.equals("Ethereum")) { 
					
					SimVehicle selectedVehicle = null;
					for (SimVehicle findNode: simVehicles)					// Find a vehicle having name of selected node
						if (findNode.getID().equals(nodeName.substring(nodeName.indexOf('#')+1))) {
							selectedVehicle = findNode;
							break;
						}
					
					if (selectedVehicle != null) {
				
						table.getItem(0).setText(new String[] {"Name", selectedVehicle.getID()});
						table.getItem(1).setText(new String[] {"Monitoring Device", selectedVehicle.getDeviceName()});
						table.getItem(2).setText(new String[] {"Monitoring Type", selectedVehicle.getDeviceType()});
						table.getItem(3).setText(new String[] {"Resolution", selectedVehicle.getResolution()});
						table.getItem(4).setText(new String[] {"Recording Time(s)", String.valueOf(selectedVehicle.getRecordingTime())});
						table.getItem(5).setText(new String[] {"Target Distance(m)", String.valueOf(selectedVehicle.getTargetDistance())});
						table.getItem(6).setText(new String[] {"Source Address", selectedVehicle.getSourceAddress()});
						
						if (selectedVehicle.getDeviceType().equals("Photo"))
							browser.setUrl(selectedVehicle.getSourceAddress());
						else
							org.eclipse.swt.program.Program.launch(selectedVehicle.getSourceAddress());							
						
					}
					
				}
				
			}
		});
		NodeTree.setBounds(10, 10, 246, 283);
		NodeTree.setLinesVisible(true);
		
		trtmNewTreeitem = new TreeItem(NodeTree, SWT.NONE);
		trtmNewTreeitem.setText("Ethereum");
		trtmNewTreeitem.setExpanded(true);
		
		txtLog = new Text(shlSelfadaptiveContractSimulator, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.MULTI);
		txtLog.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		txtLog.setBackground(SWTResourceManager.getColor(SWT.COLOR_INFO_BACKGROUND));
		txtLog.setBounds(3, 678, 800, 120);
		
		btnStartSim.addMouseListener(new MouseAdapter() {
			
			public void mouseDown(MouseEvent e) {
				
				int randomNum = ThreadLocalRandom.current().nextInt(10000, 20000);
				
				txtLog.append("Running SUMO Server...\n");
				connSumo = new SumoTraciConnection(mapPath, randomNum);
				
				try {
					connSumo.runServer();					
					connSumo.nextSimStep();												// First step is empty
										
					txtLog.append("Successfully loading sumo map!!\n");
					lblMapInfo.setText("Map siz:(" + connSumo.queryBounds().getWidth() + "," + connSumo.queryBounds().getHeight() + "), "
									+ "Total # of Vehicles:" + SASimUtil.getNumOfVehicles("./map/" + selType[0] + "/" + selType[0] + ".rou.xml"));
					
					xPosWeight = compMain.getBounds().width / connSumo.queryBounds().getWidth();
					yPosWeight = compMain.getBounds().height / connSumo.queryBounds().getHeight();
					
				} catch (Exception ex) {
					txtLog.append("Sumo Server error: " + ex.getMessage() + "\n");
				}				
				
				txtLog.append("Running Ethereum master node..." + "\n");
				String [] cmd = {HOME_DIR + '/' + txtRootDir.getText() + "/ethereum/runMasterNode.sh", txtEthDir.getText()};
				
				try {
					
					Process script_exec = Runtime.getRuntime().exec(cmd);
					BufferedReader reader = new BufferedReader(new InputStreamReader(script_exec.getInputStream()));
					
					String output;
					while ((output = reader.readLine()) != null)
						System.out.println(output);
					
				} catch (IOException ioe) {
					txtLog.append("Error: " + ioe.getMessage() + "\n");
				}
				
				txtLog.append("Successfully establishing Ethereum master node!!\n");
				
				btnNextStep.setEnabled(true);									// Set Next Step button enable
				btnAutoStep.setEnabled(true); 									// Set Auto Step button enable
				btnStartSim.setEnabled(false);									// Set Start button disable
				btnStopSim.setEnabled(true);									// Set Stop button enable
				
				TreeItem masterNodeItem = new TreeItem(trtmNewTreeitem, SWT.NONE);
				masterNodeItem.setText("Master Node");
			}
		});
		
		btnNextStep.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent e) {
				
				btnAutoStep.setEnabled(false); 									// Set Auto step to disable
				autoStepRunning = true;
				
				if (connSumo != null) {
					try {
						Collection<Vehicle> vehicles = connSumo.getVehicleRepository().getAll().values();
						
						// 1. Running Ethereum client and draw nodes to main composite
						for (Vehicle vehicle: vehicles)
							runEthereumClient(vehicle);
						
						// 2. Draw Vehicles and network lines
						drawVehicles(simVehicles);
						
						// 2. Print connected Nodes...
						for (SimVehicle simVehicle: simVehicles) {
							System.out.print("Node #" + simVehicle.getID() + "'s connected Nodes: { ");
							for (String nodeID: simVehicle.getConnectedNodes()) 
								System.out.print(nodeID + " ");
							System.out.println("}");
						}
						
						connSumo.nextSimStep();			
					} catch (IOException ioe) {
						txtLog.append("Sumo Server error: " + ioe.getMessage() + "\n");
					}
				}
				
			}
		});
		
		
		btnAutoStep.addMouseListener(new MouseAdapter() {
			
			public void mouseDown(MouseEvent e) {
				
				int stepTime = (int) SASimUtil.getLastDepartTime("./map/" + selType[0] + "/" + selType[0] + ".rou.xml");
				
				autoStepRunning = true;
				btnNextStep.setEnabled(false); 									// Set Next step button to disable
				
				for (int i = 0; i < stepTime; i++) {
				
					display.asyncExec(new Runnable() {
						
						public void run() {
						
							if (autoStepRunning) {
			
								try {

									Collection<Vehicle> vehicles = connSumo.getVehicleRepository().getAll().values();
									
									// 1. Running Ethereum client
									for (Vehicle vehicle: vehicles)
										runEthereumClient(vehicle);
									
									// 2. Draw Vehicles and network lines
									drawVehicles(simVehicles);

									// 3. Wait and go to next step
									Thread.sleep(Integer.parseInt(txtDelay.getText()));
									connSumo.nextSimStep(); 
									 
								} catch (NumberFormatException | InterruptedException | IllegalStateException | IOException e) {
									e.printStackTrace();
								}

							} 
						}
					});				
				}
			}
		});
		
		btnStopSim.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent e) {
				
				autoStepRunning = false;	
				
				try {
					Thread.sleep(Integer.parseInt(txtDelay.getText()));
				} catch (NumberFormatException | InterruptedException e1) {
					e1.printStackTrace();
				}	
				
				try {
					terminate();
				} catch (InterruptedException | IOException e1) {
					e1.printStackTrace();
				}
								
				btnNextStep.setEnabled(false);									// Set Next Step button disable
				btnAutoStep.setEnabled(false); 									// Set Auto Step button disable
				btnStartSim.setEnabled(true);									// Set Start button enable
				btnStopSim.setEnabled(false);									// Set Stop button disable
				
				trtmNewTreeitem.removeAll(); 									// Remove all items of ethereum root tree item
				settingTableItems();											// Clean Property Table				
			}
		});
		
		shlSelfadaptiveContractSimulator.addDisposeListener(new DisposeListener()  {
			public void widgetDisposed(DisposeEvent arg0)  {				
				try {
					terminate();
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
				}			
			}
		});
	}
	
	public void terminate() throws InterruptedException, IOException {
		
		if (connSumo != null) {
			connSumo.close();
			autoStepRunning = false;
			System.out.println("Closing SUMO Server...");
		}
		
		simVehicles = new ArrayList<SimVehicle>();
		
		for (Control subCompObj: compMain.getChildren())
			if (!(subCompObj instanceof Button))
				subCompObj.dispose();
		compMain.redraw();
		
		System.out.println("Terminate all ethereum nodes...");
		
		String [] cmd = {"killall", "geth"};				
		String [] removeCmd = {"rm", "-r", HOME_DIR + "/" + txtEthDir.getText()};
		
		Process script_exec = Runtime.getRuntime().exec(cmd);
		BufferedReader reader = new BufferedReader(new InputStreamReader(script_exec.getInputStream()));
		
		String output;
		while ((output = reader.readLine()) != null) {
			System.out.println(output);
		}
		
		Runtime.getRuntime().exec(removeCmd);
		System.out.println("Successfully terminated!");
	}
	
	public void runEthereumClient(Vehicle vehicle) throws IOException {

		// Is this node newly entered in the node?
		boolean isExistingVehicle = false;
		for (SimVehicle simVehicle: simVehicles) {
			if (simVehicle.getID() == vehicle.getID()) {
				isExistingVehicle = true;		
				simVehicle.setXpos(vehicle.getPosition().getX());
				simVehicle.setYpos(vehicle.getPosition().getY());
				simVehicle.findNearNodes(simVehicles);
			}
		}
		
		// Find newly entered vehicle and running ethereum client
		if (!isExistingVehicle) {
			
			txtLog.append("Vehicle ID " + vehicle.getID() + " is newly entered in the map.\n");				

			// update Newly entered node to Tree Update
			TreeItem nodeItem = new TreeItem(trtmNewTreeitem, SWT.NONE);
			nodeItem.setText("Node#" + vehicle.getID());
			
			// running new ethereum node
			Thread runEthClient = new RunEthereum(HOME_DIR + '/' + txtRootDir.getText(), vehicle.getID());
			runEthClient.start();
			
			// add Newly entered node to simVehicle
			SimVehicle newVehicleNode = new SimVehicle(vehicle.getID(), vehicle.getPosition().getX(), vehicle.getPosition().getY(), txtPeerDistance.getText(), HOME_DIR + '/' + txtRootDir.getText(), txtEthDir.getText());
			simVehicles.add(newVehicleNode);
			newVehicleNode.findNearNodes(simVehicles);
		}
	}
	
	public void drawVehicles(ArrayList <SimVehicle> simVehicles) {
		// Draw Vehicle Circle in Main composite
		compMain.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				
				Font font = new Font(display,"Arial",9,SWT.BOLD); 
				e.gc.setFont(font);
				
				for (SimVehicle simVehicle: simVehicles) {
					
					if (simVehicle.getID().equals("0"))
						e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
					else
						e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
					
					e.gc.setForeground(display.getSystemColor(SWT.COLOR_YELLOW));
					
					// Draw Network Lines
					List <String> peerNodeList = simVehicle.getConnectedNodes();
					for (String peerNode: peerNodeList) {
						
						SimVehicle targetNode = null;
						for (SimVehicle findNode: simVehicles)
							if (findNode.getID() == peerNode)
								targetNode = findNode;
								
						if (simVehicle.getID() != peerNode) {
							try {
								e.gc.drawLine((int)(simVehicle.getXpos()*xPosWeight)+20, (int)(simVehicle.getYpos()*yPosWeight)+20,
											(int)(targetNode.getXpos()*xPosWeight)+20, (int)(targetNode.getYpos()*yPosWeight)+20);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
					
					String vehID = simVehicle.getID().replace("veh", "");
					int strWith = e.gc.stringExtent(vehID).x;
					
					if (autoStepRunning) {			
						try {
							e.gc.fillOval((int)(simVehicle.getXpos() * xPosWeight)+10, (int)(simVehicle.getYpos() * yPosWeight)+10, 20, 20);
							e.gc.drawText(vehID, (int)(simVehicle.getXpos() * xPosWeight)+(20-(strWith/2)), (int)(simVehicle.getYpos() * yPosWeight)+13);		
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			 }
		});
		compMain.redraw();
	}
	
	public void makeSUMOMap(String [] selType) {
		
		mapPath = "./map/" + selType[0] + "/" + selType[0] + ".sumocfg";
		
		if (selType[0] == "KoreaUniv" | selType[0] == "NRFSeoul" | selType[0] == "CheonanIC") return;
		
		String [] createNetCmd = {HOME_DIR + '/' + txtRootDir.getText() + "/sumo/createNet.sh", selType[0], selType[1]};		
		String [] makeRouteCmd = {HOME_DIR + '/' + txtRootDir.getText() + "/sumo/makeRoute.sh", selType[0], selType[2]};
		
		try {
			
			Runtime.getRuntime().exec(createNetCmd);			
			Runtime.getRuntime().exec(makeRouteCmd);
			
			mapPath = "./map/" + selType[0] + "/" + selType[0] + ".sumocfg";
			
			txtLog.append("Successfully making map into following path: " + mapPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void settingTableItems() {

		table.removeAll();
		TableItem nameItem = new TableItem(table, SWT.NONE);
		nameItem.setText(new String[] {"Name", ""});
		TableItem deviceItem = new TableItem(table, SWT.NONE);
		deviceItem.setText(new String[] {"Monitoring Device", ""});
		TableItem typeItem = new TableItem(table, SWT.NONE);
		typeItem.setText(new String[] {"Monitoring Type", ""});
		TableItem resolutionItem = new TableItem(table, SWT.NONE);
		resolutionItem.setText(new String[] {"Resolution", ""});
		TableItem timeItem = new TableItem(table, SWT.NONE);
		timeItem.setText(new String[] {"Recording Time(s)", ""});
		TableItem distanceItem = new TableItem(table, SWT.NONE);
		distanceItem.setText(new String[] {"Target Distance(m)", ""});
		TableItem srcItem = new TableItem(table, SWT.NONE);
		srcItem.setText(new String[] {"Source Address", ""});
	}
}

class RunEthereum extends Thread {
	
	private String rootPath;	
	private String nodeID;
	
	public RunEthereum(String rootPath, String nodeID) {
		this.rootPath = rootPath;	
		this.nodeID = nodeID;
	}
	
	public void run() {
		String [] cmdCreateNode 	= {rootPath + "/ethereum/runEthNode.sh", nodeID};
		
		try {
			Runtime.getRuntime().exec(cmdCreateNode);			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}