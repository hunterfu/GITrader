package com.tim.service;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Priority;
import org.xml.sax.SAXException;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.Order;
import com.ib.client.TickAttr;
import com.ib.client.TickType;
import com.ib.contracts.FutContract;
import com.ib.contracts.StkContract;
import com.tim.dao.OrderDAO;
import com.tim.dao.PositionDAO;
import com.tim.dao.RealTimeDAO;
import com.tim.dao.ShareDAO;
import com.tim.jobs.Trading_Read;
import com.tim.model.Position;
import com.tim.model.RealTime;
import com.tim.model.Share;
import com.tim.util.ConfigKeys;
import com.tim.util.LogTWM;
import com.tim.util.PositionStates;
import com.tim.util.Utilidades;


import  com.ib.client.Contract;
import  com.ib.client.TickType;


import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EReaderSignal;


public class TIMApiGITrader extends TIMApiWrapper {

	

	SimpleDateFormat sdf = new SimpleDateFormat ("yyyyMM");
	
	private String _ConnectionHOST = "127.0.0.1";
	private int  _ConnectionPORT = 7497;
	private int _ConnectionCLIENTID = 7;
	
	
	private int LastPositionID = 0;
	public int getLastPositionID() {
		return LastPositionID;
	}
	
	public void nextValidId(int orderId) {
		// MyLog.log(Priority.INFO,"NextValid ID:" + orderId);
		LastPositionID = orderId;
	}

	public void setLastPositionID(int lastPositionID) {
		LastPositionID = lastPositionID;
	}


	private int lastOrderID = 0;
	
	
	private static double _HISTORICAL_DATA_LOW=0;
	private  static double _HISTORICAL_DATA_HIGH=0;
	private  static String  _HISTORICAL_DATA_REQUEST="";   // OK acabada.	
	public static String get_HISTORICAL_DATA_REQUEST() {
		return _HISTORICAL_DATA_REQUEST;
	}


	public static void set_HISTORICAL_DATA_REQUEST(String _HISTORICAL_DATA_REQUEST) {
		TIMApiGITrader._HISTORICAL_DATA_REQUEST = _HISTORICAL_DATA_REQUEST;
	}


	public static void set_DATE_HISTORICAL_REQUEST(String _DATE_HISTORICAL_REQUEST) {
		TIMApiGITrader._DATE_HISTORICAL_REQUEST = _DATE_HISTORICAL_REQUEST;
	}


	private  static String  _HISTORICAL_MODE_REQUEST="REAL";   // REAL, SIMULATION	
	private  static String  _CONTRACT_DATA_REQUEST="Stopped";   // [Stopped,Requested, Ended, Received,Executing]
	private static String _DATE_HISTORICAL_REQUEST = "";  //yyyymmdd  
	private static String _HISTORICAL_DATA_ERRORS_CODES = "";
	
	/* USAMOS PARA ALMACENAR LAS TRAZAS DE RESULTADOS POR BLOQUES Y BUSCAR EL MAXIMO Y MIN DESPUES */
	private  ArrayList<Double> aLowValuesHistorical =  new ArrayList<Double>();
	private ArrayList<Double> aHighValuesHistorical =  new ArrayList<Double>();
	
	/* FECHA ESTIMATIVA, NO SIRVE  COMO INFORMATIVA */
	private  static String _HISTORICAL_DATA_DATE  = "";	
	private static String _HISTORICAL_DATA_DATE_BEGINNING  = "";
	
	private String error = "";

	public String GITraderError() {
		return error;
	}

	
	
	/* ESTE DATO SIRVE PARA ALMACENAR DATOS HISTORICOS DE LA FECHA EN CUESTION 
	 * LA TWS DEVUELVE DATOS ANTERIORES A UNA FECHA DADA, NO ENTRE FECHAS, CON LO QUE SI PIDES UN DOMINGO TE DARA 
	 * DEL VIERNES */
	
	
	//private  com.ib.client.EJavaSignal _GITradeSignal  = null;
	//private EClientSocket _GITraderSocket = new EClientSocket(this, _GITradeSignal);
	/// private EClientSocket _GITraderSocket = null;
	
	/* private EClientSocket this.getClient()=null;
	private EReaderSignal m_signal=null;*/
	//private EReader reader=null;   
	
	/* private  EReaderSignal TIMApireaderSignal=null;
	private EClientSocket clientSocket=null;
	*/
	private List<Long> _lTest = null;
	
	
	private Contract __GITraderContract;
	
	

	
	public static String get_HISTORICAL_DATA_ERRORS_CODES() {
		return _HISTORICAL_DATA_ERRORS_CODES;
	}


	public static String get_DATE_HISTORICAL_REQUEST() {
		return _DATE_HISTORICAL_REQUEST;
	}


	
	
	public  void GITraderHistoricalData(int tickerId, Contract contract,
			String endDateTime, String durationStr, String barSizeSetting,
			String whatToShow, int useRTH, int formatDate) {
		
		_HISTORICAL_DATA_REQUEST = "Executing";
		
		clientSocket.reqHistoricalData(tickerId, contract, endDateTime,
				durationStr, barSizeSetting, whatToShow, useRTH, formatDate, false, null);

	}
	
	/* public Contract GITraderCreateFUTUREContract(String symbol) {
						
				return  new FutContract(symbol, "2017/04/01");
				
	}*/
	
	/* METODOS PERSONALIZADOS QUE NO ESTAN EN EL INTERFACE */
	public boolean GITradercancelOrder(int RequestID) throws InterruptedException {
		clientSocket.cancelOrder(RequestID);
		return true;
	}
	
	/* METODOS PERSONALIZADOS QUE NO ESTAN EN EL INTERFACE */
	public boolean GITradergetContractDetails(int RequestID, Contract contract)
			throws InterruptedException {
		
		_CONTRACT_DATA_REQUEST = "Executing";
		
		clientSocket.reqContractDetails(RequestID, contract);
		return true;
	}
	
	public Contract GITraderCreateSTKContract(String symbol) {
					
				return  new StkContract(symbol);
				
	}
	
	/*public TIMApiGITrader()  {
		
		TIMApiGITrader(_ConnectionHOST, _ConnectionPORT, _ConnectionCLIENTID);
		
	}*/
	
	public void GITraderOpenOrder(int orderId, Contract contract, Order order) {
		
		 LogTWM.getLog(TIMApiGITrader.class); 
		 LogTWM.log(Priority.INFO, "Open Order:" + orderId + " Contrato =" + contract.symbol() + " order=" + order.totalQuantity() + " price=" + order.lmtPrice());
		 

		 clientSocket.placeOrder(orderId, contract, order);

	}
	
	public TIMApiGITrader(String _host, int _port, int clientid)  {
		//super();			
		_ConnectionHOST = _host;
		_ConnectionPORT = _port;
		_ConnectionCLIENTID = clientid;		
		
		/* TIMApireaderSignal = new EJavaSignal();
		TIMApiclientSocket = new EClientSocket(this, TIMApireaderSignal);
		*/
		_lTest = new ArrayList();
		_lTest.add(new Long(1));
				
		/* m_signal = new EJavaSignal();
		this.getClient() = new EClientSocket(this, m_signal);*/
		//reader = new EReader(this.getClient(), this.getSignal());
		
	//	_GITraderSocket = this.getClient();
	}
	
	public void GITrade() {
		clientSocket.eConnect(_ConnectionHOST, _ConnectionPORT, _ConnectionCLIENTID);
	}

	public void disconnectFromTWS() {
		if (clientSocket.isConnected()) {
			clientSocket.eDisconnect();
		}
	}
	
	
	@Override
	public void tickPrice(int tickerId, int field, double price, TickAttr attribs) {
		// TODO Auto-generated method stub
		
		com.tim.model.Order MyOrder = null;

		if (field==ConfigKeys._TICKTYPE_CLOSE)
		{
			
			if (MyOrder == null) {
				LogTWM.getLog(TIMApiGITrader.class);
				LogTWM.log(Priority.ERROR, "No se encuentra el ID " + tickerId);
				return;
			}
			//saveDataLastRealTime(MyOrder.getShareID().intValue(), "close_value", price); 			
		}
		if (field==ConfigKeys._TICKTYPE_LAST) {

				MyOrder = OrderDAO.getOrder(tickerId);
			if (MyOrder == null) {
				LogTWM.getLog(TIMApiGITrader.class);
				LogTWM.log(Priority.ERROR, "No se encuentra el ID " + tickerId);
				return;
			}
			RealTimeDAO oRealDAO = new RealTimeDAO();
			oRealDAO.addRealTime(MyOrder.getShareID().intValue(), price);
			//Share Mishare = ShareDAO.getShare(MyOrder.getShareID());
			OrderDAO.updateStatusOrder(tickerId, 1);

		}

		
		
		
	}


	public void GITraderGetRealTimeContract(int RequestID, Contract contract)
			throws InterruptedException {
		
		clientSocket.reqMktData(RequestID, contract,  "", false, false, null);
		//m_client.reqHistoricalData(4001, _contractAPI31, formatted, "1 M", "1 day", "MIDPOINT", 1, 1, false, null);


	}
	
	public void error(int one, int two, String str) {

		
		/* LogTWM.getLog(TIMApiWrapper.class);
		LogTWM.log(Priority.FATAL, "error lectura : [" + one + "," + two
				+ "," + str + "]");
		*/
		
		/* 1. LECTURA DE DATOS --> INFORMACION ERROR, INTRODUCIMOS OBSERVACIONES 
		 * 2. ORDENES , OPEN ORDERS...CLOSE ORDERS */
		
		
		if (two>=0 && one>=0)  // errores operativa - lectura
		{
			
		java.sql.Timestamp FechaError = new Timestamp(Calendar.getInstance().getTimeInMillis());	    			
		
		SimpleDateFormat sdf2 = new SimpleDateFormat();
		sdf2.applyPattern("dd-MM-yyyy HH:mm:ss");
		 
		Position _ErrorPosition = null;
		_ErrorPosition = PositionDAO.getPosition(one);
		
		Share oErrorShare = null;
		
		if (_ErrorPosition!=null)  // error en una posicion dada
		{
			oErrorShare = ShareDAO.getShare(_ErrorPosition.getShareID());
			
			if (_ErrorPosition.getPrice_sell()!=null && _ErrorPosition.getPrice_sell().compareTo(new Double(0))>0) 
			{
				// INTENTO DE salir --> NO SABEMOS LO QUE HACER
			}
			else   // entrada???.. En ppio se quedan los errores gestionados en el orderstatus.
			{
				
			  	LogTWM.getLog(TIMApiGITrader.class);
				LogTWM.log(Priority.FATAL, "error operativa : [" + one + "," + two
						+ "," + str + "]");
				LogTWM.log(Priority.FATAL, "error order  : " + oErrorShare.getSymbol());
				oErrorShare.setActive_trading(new Long(0));  // desactivamos trading.
				//Actualizamos campos de errores.
				oErrorShare.setLast_error_data_trade(FechaError + "|" +  str + "[N. Error " + two + "]");  // desactivamos trading.
				
				
				_ErrorPosition.setPending_cancelled(1);  // supuestamente se cancela SOLA.
				PositionDAO.deletePositionByPositionID(_ErrorPosition);

				/* actualizamos datos error de operativa */
				ShareDAO.updateShare(oErrorShare);
				/* fin actualizamos datos error de operativa */
				
				// Actualizamos estado de la posici�n con Cancelled o similar
				// Ser�a interesante que apareciera en la consola pero puede dar problemas (Gris???)
			
			}
			
				
		}
		else   // supuestamente error lectura de datos en tiempo real.
			   // METEMOS EL 25-12-2013 un  job verificador que pide el contract detail con variable de estado asociada.
			
		{
			
			LogTWM.getLog(TIMApiGITrader.class);
			LogTWM.log(Priority.FATAL, "error lectura [HISTORICAL_DATA: " + _HISTORICAL_DATA_REQUEST + "] : [" + one + "," + two
					+ "," + str + "]");
			

			/* SIEMPRE VA A VER _ErrorOrder con errores de tiempo real.
			 * TENEMOS UN PROBLEMA DE CONECTIVIDAD EN EL TICK --> PASA POR AQUI.
			 * EL JOB DE VERIFICACION COMPRUEBA PARA STOCKS SI EL ACTIVO ES VALIDO, PERO
			 * PARA UN FUTURO VALIDO PERO CON EL EXPIRATION DATE ERRONEO O PASADO, NO LO VERIFICA,
			 * LO IMPLEMENTAMOS AQUI.
			 * AQUI CONTROLAMOS DOS COSAS,
			 * 1. JOB DE VERIFICACION PARA ANULAR LOS STOCKS INVALIDOS.
			 * 2. DESCARGA DE TIEMPO REAL SOLO PARA FUTUROS POR LA RAZON ANTERIOR,. 
			 * Filtramos los del CONTRACTDETAILS EXCLUSIVAMENTE. ES DECIR, ESTADO A ARRANCADO.
			 * [Stopped,Requested, Ended, Received, Executing] y SI ES UN FUTURO.
			 * _HISTORICAL_DATA_REQUEST "Executing" para controlar los errores  
			 * 162	Historical market data Service error message.				
			 * 165	Historical market Data Service query message.
			 * 
			 *   */
			
			
			if (one!=-1)
			{	
			
				
				StringBuilder _stbB = new StringBuilder();
				_stbB.append(one);
				_stbB.append("|");
				_stbB.append(two);
				_stbB.append("|");
				_stbB.append(str);
				
				
				
				_HISTORICAL_DATA_ERRORS_CODES =_stbB.toString(); 
				
				
				com.tim.model.Order _ErrorOrder = OrderDAO.getOrder(one);
				if (_ErrorOrder!=null)
				{
					
					
					
					oErrorShare = ShareDAO.getShare(_ErrorOrder.getShareID());
					
					
					if (oErrorShare!=null 
							&& (oErrorShare.getSecurity_type().equalsIgnoreCase(ConfigKeys.SECURITY_TYPE_FUTUROS)
							   || _HISTORICAL_DATA_REQUEST.equals("Executing")  
							   		|| _CONTRACT_DATA_REQUEST.equals("Requested") 
							   			|| _CONTRACT_DATA_REQUEST.equals("Executing")))
					{
					
					
						LogTWM.log(Priority.FATAL, "error order  : " + oErrorShare.getSymbol());
						
						
						// DESACTIVAMOS SIEMPRE QUE NO SEA POR PACING VIOLATIONS
						//162	Historical market data Service error message.		
						if (two!=162)
						{
							
							oErrorShare.setActive(new Long(0));  // desactivamos lectura.
							oErrorShare.setLast_error_data_read("[" + oErrorShare.getSymbol() + "]" + FechaError + "|" + str + "[N. Error " + two + "]");  // desactivamos
							oErrorShare.setDate_contract_verified(FechaError);
							/* actualizamos datos error de operativa */
							ShareDAO.updateShare(oErrorShare);
						
						}
							
						
						/* fin actualizamos datos error de operativa */
					}
					
				}
			} // fin one <> -1
			
		}
		
		
			
		}
		else // fin errores operativa - lectura. Conectividad 
		{
			error =   str + "[N. Error " + two + "]";
			
		}
			
			
		
		

	}
	public void error(String str) {
		LogTWM.getLog(TIMApiGITrader.class);
		LogTWM.log(Priority.FATAL, "error consoleTWS1: [" + str + "]");
	}
	
	public void GITraderConnetToTWS() throws InterruptedException {
		
		//! [connect]
		//! [ereader]
		
		clientSocket.eConnect(_ConnectionHOST, _ConnectionPORT, _ConnectionCLIENTID);
		
		EReader reader2 = new EReader(clientSocket, readerSignal);   
		
		reader2.start();
		
		
		//An additional thread is created in this program design to empty the messaging queue
		new Thread(() -> {
		    while (clientSocket.isConnected()) {
		    	readerSignal.waitForSignal();
		        try {
		            reader2.processMsgs();
		        } catch (Exception e) {
		            System.out.println("Exception: "+e.getMessage());
		        }
		    }
		}).start();
		
		//TIMApiclientSocket.eConnect(_ConnectionHOST, _ConnectionPORT, _ConnectionCLIENTID);

		
	}
	
	@Override
	public void orderStatus(int orderId, String status, double filled,
			double remaining, double avgFillPrice, int permId, int parentId,
			double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
		
			


		/* LogTWM.getLog(TIMApiWrapper.class);
		LogTWM.log(Priority.INFO,"order status: orderId=" + orderId + " clientId=" + clientId + " permId=" + permId +
	    	        " status=" + status + " filled=" + filled + " remaining=" + remaining +
	    	        " avgFillPrice=" + avgFillPrice + " lastFillPrice=" + lastFillPrice +
	    	        " parent Id=" + parentId +  "|" + whyHeld );
	    	*/
	    //	MyLog.log(Priority.INFO,"Hola1");
			
			java.sql.Timestamp FechaError = new Timestamp(Calendar.getInstance().getTimeInMillis());	    			
		
			SimpleDateFormat sdf2 = new SimpleDateFormat();
			sdf2.applyPattern("dd-MM-yyyy HH:mm:ss");
		 
        	
			double priceStopLost = 0;
			double priceStopProfit = 0;
			
			DecimalFormat sNumero = new DecimalFormat();
			sNumero.applyPattern("######,##");
			
			boolean bIsSellOperation = false;  //ENTRADA O SALIDA, TERMINO ERRONEO
			boolean isDelete = false;   // cuando sea compra cancelada, nos la cargamos.
	    	
	    	try {
	    		
	    		Position MiPosicion= null;
	    		Share MySharePosition= null;
	    		
	    		MiPosicion=  (Position) PositionDAO.getPosition(orderId);
	    		
	    		// SI ES NULL, QUIERE DECIR QUE PUEDE VENIR UNA OPERACION DE VENTA...LA BUSCAMOS.
	    		if (MiPosicion==null)
	    		{    			
	    			
	    			MiPosicion=  (Position) PositionDAO.getPositionByIdSell(orderId);
	    			if (MiPosicion==null)
		    		{    				    		
	    				LogTWM.log(Priority.INFO,"Error Execution Details order not found for Order Key:" + orderId);
	    				return;
		    		}	
	    			else
	    				bIsSellOperation = true;
	    		}
	    		 
	    		MySharePosition= ShareDAO.getShare(MiPosicion.getShareID());
	    		
	    		/* DETECTAMOS SI ES UNA ORDER DE COMPRA O DE VENTA. VERIFICAMOS SI HA CAMBIADO . VERIFICAR SI HAY UNA ORDEN DE COMPRA PREVIA. */
	    		boolean changed = false;
	    		if (!bIsSellOperation)   //ENTRADA OPERATION
	    		{	
	    			if (MiPosicion.getState_buy()==null || (MiPosicion.getState_buy()!=null && !MiPosicion.getState_buy().toLowerCase().equals(status.toLowerCase())))
	    			changed = true;
	    		}
	    		else  // SALIDA OPERATION
	    		{
	    			if (MiPosicion.getState_sell()==null || (MiPosicion.getState_sell()!=null && !MiPosicion.getState_sell().toLowerCase().equals(status.toLowerCase())))
		    			changed = true;
	    		}
	    		
	    		
	    		if (changed)
	    		{	    			
	    			// controlamos todas las canceladas
	    			// LogTWM.log(Priority.INFO,"Change: " + changed + ",StateBuy, StateSell,status:" + MiPosicion.getState_buy() + "," +  MiPosicion.getState_sell()  +  "," + status);
	    			
	    			
	    			if (!bIsSellOperation)   // ENTRADA OPERATION ... CANCELLED? --> LA BORRAMOS PARA QUE NO CONSTE
	    			{
	    				

	    				
	    				MiPosicion.setState_buy(status);
	    				/* cancelada compra  */
	    				/* 21.09.2013 QUITO EL OR DE INACTIVE PARA CONTROLARLO EN LA RUTINA DE ERRORES 
	    				 * || 
	    						PositionStates.statusTWSCallBack.Inactive.toString().equals(status)
	    				 */
	    				if (PositionStates.statusTWSCallBack.Cancelled.toString().equals(status))
			    		{			    						    		
	    					
	    					// procedemos a borrarla y desactivar
	    					/* FUTURO VENCIDO D-1 DEL VENCIMIENTO */
	    				
	    					//Actualizamos campos de errores.
	    					MySharePosition.setActive(new Long(0));
	    					MySharePosition.setActive_trading(new Long(0));
	    					MySharePosition.setLast_error_data_trade(sdf2.format(FechaError));  // desactivamos trading.
	    					
	    					ShareDAO.updateShare(MySharePosition);
	    					
	    					
	    					PositionDAO.deletePositionByPositionID(MiPosicion);
	    					isDelete = true;
			    			//MiPosicion.setState(PositionStates.status.CANCEL_BUY.toString());
			    			
			    		}
	    				
	    				/* OJO, PUEDEN SER VENTAS/COMPRAS  PARCIALES..ENTRADA...SOLO OPERACIONES TOTALES */
	    				if (PositionStates.statusTWSCallBack.Filled.toString().equals(status))
			    		{
			    			
			    			java.sql.Timestamp FechaCompra = new Timestamp(Calendar.getInstance().getTimeInMillis());	    			
			    			MiPosicion.setDate_real_buy(FechaCompra) ;    // OJO TIMESTAMP.			    			
			    			MiPosicion.setPrice_real_buy(avgFillPrice);  // cogemos el avg que nos manda el TWS
			    			
			    			// ACTUALIZAMOS EL PRECIO DE SALIDA CON EL PORCENTAJE PORQUE ES EL VALOR QUE TRATAMOS
			    			// vemos el tipo de operacion
			    			if (MiPosicion.getType().equals(PositionStates.statusTWSFire.SELL.toString()))  //short
			    			{
			    				priceStopLost = avgFillPrice +  (avgFillPrice *  MiPosicion.getSell_percentual_stop_lost());
				    			priceStopProfit = avgFillPrice  - (avgFillPrice *  MiPosicion.getSell_percentual_stop_profit());
			    				
			    			}
			    			else  //long
			    			{
			    				priceStopLost = avgFillPrice  - (avgFillPrice *  MiPosicion.getSell_percentual_stop_lost());
				    			priceStopProfit = avgFillPrice  + (avgFillPrice *  MiPosicion.getSell_percentual_stop_profit());
			    			}
			    				 
			    			
			    			MiPosicion.setSell_price_stop_lost(Utilidades.RedondeaPrice(priceStopLost));
			    			MiPosicion.setSell_price_stop_profit(Utilidades.RedondeaPrice(priceStopProfit));
			    			
			    			
			    			MiPosicion.setState(PositionStates.status.BUY_OK.toString());
			    			
			    		}	    				
	    						
	    			}
	    			else  // SALIDA OPERATION..  OJO, PUEDEN SER VENTAS/COMPRAS  PARCIALES.. */
	    			{
	    				
	    				
	    				if (PositionStates.statusTWSCallBack.Cancelled.toString().equals(status))
			    		{			    						    		
	    					MiPosicion.setState_sell(null);
	    					MiPosicion.setDate_sell(null);
	    					MiPosicion.setDate_real_sell(null);
	    					MiPosicion.setLimit_price_sell(null);
	    					MiPosicion.setPositionID_tws_sell(null);
	    					MiPosicion.setPrice_real_sell(null);
	    					MiPosicion.setPrice_sell(null);
	    					MiPosicion.setRealtimeID_sell_alert(null);
	    					MiPosicion.setStrategyID_sell(null);
	    					MiPosicion.setState_sell(status);	 
	    					/*MiPosicion.setSell_price_stop_lost(null);
	    					MiPosicion.setSell_price_stop_lost(sell_price_stop_lost);
	    					*/
	    					
	    					// es una venta cancelada, restauramos los valores para seguir vendiendo.	    					
			    			//MiPosicion.setState(PositionStates.status.CANCEL_BUY.toString());
			    			
			    		}
	    				
	    				
	    				/* PUEDE RETORNAR FILLED MAS DE UNA VEZ*/
	    				/* EN LAS OPERACIONES PARCIALES, DESPUES DE FILLED, PONGO EL STATESELL A NULL
	    				 * PARA PODER SEGUIR VENDIENDO. 
	    				 * PASARIA POR AQUI Y ACTUALIZARIA DOS VECES ERROREO EL CAMPO OPERATIONS.
	    				 */
	    				if (PositionStates.statusTWSCallBack.Filled.toString().equals(status)
	    						&& MiPosicion.getState_sell()!=null)
			    		{
	    					
			    			java.sql.Timestamp FechaVenta = new Timestamp(Calendar.getInstance().getTimeInMillis());	    			
			    			
			    			//SimpleDateFormat sdf2 = new SimpleDateFormat();
			    			sdf2.applyPattern("HH:mm:ss");
			    			
			    			StringBuilder StrOperaciones = new StringBuilder();
			    			
			    			StrOperaciones.append("(" + avgFillPrice + "|" + sdf2.format(FechaVenta) + "|" +  MiPosicion.getShare_number_to_trade() + "|" + MiPosicion.getStrategyID_sell() + ")");
			    			
			    			// hay operacion previa
			    			String _Operation = "";
			    			
			    			if (MiPosicion.getTrading_data_operations()!=null &&  MiPosicion.getTrading_data_operations().equals("")==false)
			    			{
			    				_Operation = MiPosicion.getTrading_data_operations() + ";";
			    			}
			    			_Operation += StrOperaciones.toString();
			    			
			    			MiPosicion.setTrading_data_operations(_Operation);
			    			
			    			//LogTWM.log(Priority.INFO,"Updating OutPut Operation.. " + MySharePosition.getSymbol());
			    			
			    			/* acumulo las acciones vendidas y a vender en la operativa */
			    			int _RemaingShares = MiPosicion.getShare_number_to_trade().intValue() +MiPosicion.getShare_number_traded().intValue();
			    			int _TotalShares = MiPosicion.getShare_number().intValue();
			    			
			    			MiPosicion.setShare_number_traded(new Long(_RemaingShares));
			    			MiPosicion.setShare_number_to_trade(MiPosicion.getShare_number() - MiPosicion.getShare_number_traded());
			    			
			    			/* NO ES EL SELL OK y PRECIOS REALES NO SE GUARDAN EN LOS CAMPOS hasta que el numero de acciones vendidas quede cerrado*/
			    			if (_RemaingShares==_TotalShares)   // ya no hay =
			    			{	
			    				MiPosicion.setState_sell(status);	 
			    				MiPosicion.setDate_sell(FechaVenta) ; // OJO TIMESTAMP.
			    				MiPosicion.setDate_real_sell(FechaVenta) ; // OJO TIMESTAMP.			    			
				    			MiPosicion.setPrice_real_sell(avgFillPrice);  // cogemos el avg que nos manda el TWS
			    				MiPosicion.setState(PositionStates.status.SELL_OK.toString());
			    			}
			    			else   //  hay acciones pendientes para salir..actualizo el stop de beneficio para no dispararse
			    			{
			    				// SE PUEDEN DAR DOS COSAS,
			    				// 1. QUE EL STOP PROFIT SALTO POR DEBAJO DEL INICIAL--> DEJAMOS EL INICIAL
			    				// 2. QUE EL STOP PROFIT SALTO POR ENCIMA DEL INICIAL --> SIGUE EN TENDENCIA, LOS MULTIPLICAMOS POR UN 50%
			    				
			    				MiPosicion.setState_sell(null);	
			    				
			    				double priceStopProfitAperturaPosicion = 0;
			    				double priceNewStopProfit = 0;
			    				
			    				
			    				if (MiPosicion.getType().equals(PositionStates.statusTWSFire.SELL.toString()))  //short
				    			{
			    					// cojo el original
			    					priceStopProfitAperturaPosicion = MiPosicion.getPrice_real_buy()  - (MiPosicion.getPrice_real_buy() *  MySharePosition.getSell_percentual_stop_profit());
			    					// si el original es mayor...subio la accion mucho, la tendencia es seguir, le subo un 50% para que no salte el resto de la posicion 
			    					if (priceStopProfitAperturaPosicion > MiPosicion.getSell_price_stop_profit().doubleValue())
			    					{
			    						priceNewStopProfit = MiPosicion.getSell_price_stop_profit().doubleValue() - (MiPosicion.getSell_price_stop_profit().doubleValue() *0.5);
			    						priceNewStopProfit  = priceNewStopProfit /100;
			    						//MiPosicion.setSell_percentual_stop_profit(Utilidades.RedondeaPrice(priceNewStopProfit));
			    						
			    					}
			    					else  // restauramos el original
			    					{
			    						priceNewStopProfit = priceStopProfitAperturaPosicion;
			    									    						
			    					}
				    			}
				    			else  //long position
				    			{		
				    				// cojo el original
			    					priceStopProfitAperturaPosicion = MiPosicion.getPrice_real_buy()  + (MiPosicion.getPrice_real_buy() *  MySharePosition.getSell_percentual_stop_profit());
			    					// si el original es menor...subio la accion mucho, la tendencia es seguir, le subo un 50% para que no salte el resto 
			    					if (priceStopProfitAperturaPosicion < MiPosicion.getSell_price_stop_profit().doubleValue())
			    					{
			    						priceNewStopProfit = MiPosicion.getSell_price_stop_profit().doubleValue() + (MiPosicion.getSell_price_stop_profit().doubleValue() *0.5);
			    						priceNewStopProfit  = priceNewStopProfit / 100;
			    						//MiPosicion.setSell_percentual_stop_profit(Utilidades.RedondeaPrice(priceNewStopProfit));
			    						
			    					}
			    					else  // restauramos el original
			    					{
			    						priceNewStopProfit = priceStopProfitAperturaPosicion;
			    									    						
			    					}
			    				}
			    				
			    				MiPosicion.setSell_price_stop_profit(Utilidades.RedondeaPrice(priceNewStopProfit));
			    				
			    				//LogTWM.log(Priority.INFO,"New Profit Stop.. " + Utilidades.RedondeaPrice(priceNewStopProfit));
			    				
			    				
			    				
			    			}
			    			
			    			// ACTUALIZAMOS EL PRECIO DE SALIDA CON EL PORCENTAJE.
			    			/*priceStopLost = avgFillPrice  + (avgFillPrice *  MiPosicion.getSell_percentual_stop_lost());
			    			priceStopProfit = avgFillPrice  - (avgFillPrice *  MiPosicion.getSell_percentual_stop_profit());*/
			    			/* DOS DECIMALES PRECIOS DE SALIDA LOST Y PROFIT */
			    			/*MiPosicion.setSell_price_stop_lost(Utilidades.RedondeaPrice(priceStopLost));
			    			MiPosicion.setSell_price_stop_profit(Utilidades.RedondeaPrice(priceStopProfit));*/
			    			
			    			
			    		}	    
	    				
	    			}
	    			if (!isDelete)  // compras canceladas se borran.
	    			{	
	    				PositionDAO.updatePositionByPositionID(MiPosicion);
	    			}
	    		}	
		    	
			} catch (Exception ex) {
				LogTWM.log(Priority.ERROR,"Error Order Status:" + ex.getMessage() + "" + ex.getStackTrace().toString());
			}
			//	
			
		}
	
	
	public void GITraderDisconnectFromTWS() throws InterruptedException {
		clientSocket.eDisconnect();

	}
	public boolean GITraderTWSIsConnected() throws InterruptedException {
		return clientSocket.isConnected();
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		//! [connect]
		//! [ereader]
		//final EReader reader = new EReader(this.getClient(), m_signal);
	
		TIMApiWrapper wrapper = new TIMApiWrapper();
		
		
		final EClientSocket m_client = wrapper.getClient();
		final EReaderSignal m_signal = wrapper.getSignal();
		//! [connect]
		m_client.eConnect("127.0.0.1", 7497, 3);
		//! [connect]
		//! [ereader]
		final EReader reader = new EReader(m_client, m_signal);   
		
		reader.start();
		
		
		//An additional thread is created in this program design to empty the messaging queue
		new Thread(() -> {
		    while (m_client.isConnected()) {
		        m_signal.waitForSignal();
		        try {
		            reader.processMsgs();
		        } catch (Exception e) {
		            System.out.println("Exception: "+e.getMessage());
		        }
		    }
		}).start();
		//! [ereader]
		// A pause to give the application time to establish the connection
		// In a production application, it would be best to wait for callbacks to confirm the connection is complete
		Thread.sleep(1000);

		Contract  _contractAPI3 =  new StkContract("AAPL");
		_contractAPI3.symbol("AAPL");
		_contractAPI3.secType("STK");
		_contractAPI3.exchange("ISLAND");
		_contractAPI3.currency("USD");
		
		
		
	/* 	Contract contract = new Contract();
		contract.symbol("DAX");
		contract.secType("FUT");
		contract.currency("EUR");
		contract.exchange("DTB");
		contract.lastTradeDateOrContractMonth("201709");
		contract.multiplier("5");
		*/
		FutContract  _contractAPI4 =  new FutContract("ES","201709");
		_contractAPI4.exchange("GLOBEX");
		_contractAPI4.currency("USD");
		_contractAPI4.lastTradeDateOrContractMonth("201709");
		
		FutContract  _contractAPI5 =  new FutContract("ESTX50","201709");
		_contractAPI5.exchange("DTB");
		_contractAPI5.currency("EUR");
		_contractAPI5.lastTradeDateOrContractMonth("201709");
		
		
		//! [reqHeadTimeStamp]

		//! [cancelHeadTimestamp]
		
		//! [cancelHeadTimestamp]
		
		//! [reqhistoricaldata]
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -6);
		SimpleDateFormat form = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		String formatted = form.format(cal.getTime());
		m_client.reqHistoricalData(4001, _contractAPI3, formatted, "1 M", "1 day", "MIDPOINT", 1, 1, false, null);
		m_client.reqHistoricalData(4002, _contractAPI3, formatted, "10 D", "1 min", "TRADES", 1, 1, false, null);
		Thread.sleep(2000);
		/*** Canceling historical data requests ***/
		m_client.cancelHistoricalData(4001);
		m_client.cancelHistoricalData(4002);
		//! [reqhistoricaldata]
			
		
		

		//_APIGTrader.
		
		m_client.reqMktData(18006, _contractAPI5, "", false, false, null);
		//tickDataOperations(wrapper.getClient());
		//orderOperations(wrapper.getClient(), wrapper.getCurrentOrderId());
		//contractOperations(wrapper.getClient());
		//hedgeSample(wrapper.getClient(), wrapper.getCurrentOrderId());
		//testAlgoSamples(wrapper.getClient(), wrapper.getCurrentOrderId());
		//bracketSample(wrapper.getClient(), wrapper.getCurrentOrderId());
		//bulletins(wrapper.getClient());
		//reutersFundamentals(wrapper.getClient());
		//marketDataType(wrapper.getClient());
		//historicalDataRequests(wrapper.getClient());
		//accountOperations(wrapper.getClient());
		//newsOperations(wrapper.getClient());
		//marketDepthOperations(wrapper.getClient());
		//rerouteCFDOperations(wrapper.getClient());
		//marketRuleOperations(wrapper.getClient());
		//tickDataOperations(wrapper.getClient());
		//pnlSingle(wrapper.getClient());
		//continuousFuturesOperations(wrapper.getClient());

		Thread.sleep(100000);
		m_client.eDisconnect();

		
/* 
		TIMApiGITrader _APIGTrader = new TIMApiGITrader();
		_APIGTrader.GITraderConnetToTWS();
		_APIGTrader.GITraderGetRealTimeContract(1001, _contractAPI3);
		_APIGTrader.GITraderGetRealTimeContract(1002, _contractAPI2);
		_APIGTrader.GITraderGetRealTimeContract(1003, _contractAPI);
		

		/*** Canceling historical data request for continuous futures ***/
		//client.cancelHistoricalData(18002);

		//tickDataOperations(wrapper.getClient());
		//orderOperations(wrapper.getClient(), wrapper.getCurrentOrderId());
		//contractOperations(wrapper.getClient());
		//hedgeSample(wrapper.getClient(), wrapper.getCurrentOrderId());
		//testAlgoSamples(wrapper.getClient(), wrapper.getCurrentOrderId());
		//bracketSample(wrapper.getClient(), wrapper.getCurrentOrderId());
		//bulletins(wrapper.getClient());
		//reutersFundamentals(wrapper.getClient());
		//marketDataType(wrapper.getClient());
		//historicalDataRequests(wrapper.getClient());
		//accountOperations(wrapper.getClient());
		//newsOperations(wrapper.getClient());
		//marketDepthOperations(wrapper.getClient());
		//rerouteCFDOperations(wrapper.getClient());
		//marketRuleOperations(wrapper.getClient());
		//tickDataOperations(wrapper.getClient());
		//pnlSingle(wrapper.getClient());
		//continuousFuturesOperations(wrapper.getClient());

		
	//	_APIGTrader.disconnectFromTWS();
	}
	
	/* private  void connect(int clientId) {
		String host = System.getProperty("jts.host");
		host = host != null ? host : "";
		this.getClient().eConnect(host, 7497, clientId);
		
        final EReader reader = new EReader(this.getClient(), m_signal);
        
        reader.start();
       
		new Thread(() -> {
            while (this.getClient().isConnected()) {
                m_signal.waitForSignal();
                try {
                    SwingUtilities.invokeAndWait(() -> {
                                try {
                                    reader.processMsgs();
                                } catch (IOException e) {
                                    error(e);
                                }
                            });
                } catch (Exception e) {
                    error(e);
                }
            }
        }).start();
	}

	public void disconnect() {
		this.getClient().eDisconnect();
	}
	*/
	protected static void saveDataLastRealTime(int shareID, String field,
			double value) {

		RealTime oLastRealTime = RealTimeDAO.getLastRealTime(shareID);
		if (oLastRealTime != null) {
			// MyLog.log(Priority.INFO, "uPDATE rEALtIME " + shareID + "|" +
			// field + "|" + value);
			RealTimeDAO.updateRealTimeByField(oLastRealTime.getRealtimeID()
					.intValue(), field, value);
		}

	}


	public static String get_CONTRACT_DATA_REQUEST() {
		return _CONTRACT_DATA_REQUEST;
	}


	public static void set_CONTRACT_DATA_REQUEST(String _CONTRACT_DATA_REQUEST) {
		TIMApiGITrader._CONTRACT_DATA_REQUEST = _CONTRACT_DATA_REQUEST;
	}	
	 
		
	
	


}
