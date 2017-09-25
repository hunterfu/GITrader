package com.tim.model;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Priority;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.contracts.FutContract;
import com.ib.contracts.StkContract;
import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.log4j.Log4jMLog;
import com.tim.dao.OrderDAO;
import com.tim.dao.PositionDAO;
import com.tim.dao.RealTimeDAO;
import com.tim.service.TIMApiGITrader;
import com.tim.util.ConfigKeys;
import com.tim.util.LogTWM;
import com.tim.util.PositionStates;
import com.tim.util.TWSMail;
import com.tim.util.Utilidades;

public class  StrategySellProfit  extends Strategy  {
	
	private Double ValueToSell =0.0;
	double operation_amount = 0;	
	double profit_amount = 0;	
	double profit_rate =0; // porcentaje de beneficio 3%..proviene de la estrategia.
	private List<Position> lMyActualPosition =null;
	private Position MyActualPosition =null;
	
	private Long RealTimeID= new Long(0);
	
	private LogTWM MyLog; 

	
	public StrategySellProfit() {
		super();
		this.JSP_PAGE = "/jsp/admin/strategies/strategy_stopprofit.jsp";
		// TODO Auto-generated constructor stub
	}

	public boolean isActive() {
		// TODO Auto-generated method stub
		return (this.getActive().equals(new  Integer(1)));
	}

	public boolean Verify(Share ShareStrategy, Market oMarket) {
		// TODO Auto-generated method stub
		
		boolean verified = false;
		
		try
        {
			
		LogTWM.getLog(StrategySellProfit.class);			
				
		
		// maximos y minimos  
		RealTime oShareLastRTime = (RealTime) RealTimeDAO.getLastRealTime(ShareStrategy.getShareId().intValue());
		if (oShareLastRTime!=null)
		{	
			//double ValueToBuy =0.0;  
			//ValueToSell = oShareLastRTime.getValue();
			/*  cogemos el tiempo real, verificamos el beneficio 
			sobre la posicion abierta.
			ojo con este metodo q no tengo claro si estabien modelizado
			 */
			if (PositionDAO.ExistsPositionToSell(ShareStrategy.getShareId().intValue()))			
			{
				// supuestamente una abierta.
				lMyActualPosition = (List<Position>) PositionDAO.getPositions(ShareStrategy.getShareId().intValue(), true);
				if (lMyActualPosition!=null && lMyActualPosition.size()>0)   // operacion abierta
				{
					MyActualPosition = lMyActualPosition.get(0);
					/* CONTROLAMOS EL BENEFICIO */
					
										
					operation_amount = MyActualPosition.getSell_price_stop_profit();
					profit_amount = oShareLastRTime.getValue();
					
					
					
					boolean bExistsProfit = false;

					//hay beneficio..vemos que operacion, corto o no (ojo que el corto nos funciona con el patron SELL, no se si es SHORT
					if (MyActualPosition.getType().equals(PositionStates.statusTWSFire.BUY.toString()))  // operacion de compra normal..??
					{
						bExistsProfit = (profit_amount > operation_amount);						
						
					}
					else
					{
					
						bExistsProfit = (profit_amount < operation_amount);  // operacion a corto
						
					}		
							
					if (bExistsProfit)
					{
						
						LogTWM.log(Priority.INFO, "Valores de BBDD, StopProfit " + MyActualPosition.getSell_price_stop_profit() +
								",RealTime:" + oShareLastRTime.getValue());

					
						// si esta la estrategia con una valor de rentabilidad 
						
						/*
						 *  3.01.2013..cambiamos el dato y lo cogemos de la posicion
						 *  if (ShareStrategy.getSell_percentual_stop_profit_position()!=null &&  
								profit_rate >= ShareStrategy.getSell_percentual_stop_profit().doubleValue())
						
						if (ShareStrategy.getSell_percentual_stop_profit_position()!=null &&  
							profit_rate >= MyActualPosition.getSell_percentual_stop_profit().doubleValue())
						
						{*/	
							LogTWM.log(Priority.INFO, "Detectada salida por beneficio de " + 
									ShareStrategy.getName() + ",ProfitValue:" + operation_amount + ",CurrentPrice:" + profit_amount );
							
							ValueToSell = oShareLastRTime.getValue();
							RealTimeID  = oShareLastRTime.getRealtimeID();
							verified = true;
					/*	} */
					}
				}
				
			}
			
			
			
		}
		
        }
		catch (Exception er)
        {
			LogTWM.log(Priority.ERROR, er.getMessage());
			er.printStackTrace();	
			verified = false;
        }

		return verified;
	}
	
	public boolean Execute(Market oMarket, Share ShareStrategy, TIMApiGITrader oTIMApiWrapper) {
		// TODO Auto-generated method stub
		try
        {							
			// hace falta???????? ..creo que si, para tener control sobre la operacion de compra /venta 

			SimpleDateFormat sdf = new SimpleDateFormat ("yyyyMM");

			
			boolean bIsFutureStock = ShareStrategy.getSecurity_type().equals(ConfigKeys.SECURITY_TYPE_FUTUROS)  && ShareStrategy.getExpiry_date()!=null;

			String _Expiration = "";
  		    if (bIsFutureStock)
				_Expiration = sdf.format(ShareStrategy.getExpiry_date());
  		    
  		    Contract oContrat = null;
  		    
  		  if (ShareStrategy.getSecurity_type().equals("FUT"))
  		  {
  			oContrat = new FutContract( ShareStrategy.getSymbol(), _Expiration);
			//oContrat.multiplier(String.valueOf(oShare.getMultiplier()));		    					
			oContrat.exchange(ShareStrategy.getExchange());
			oContrat.currency(oMarket.getCurrency());
  		  }
			else		    					
				oContrat = new StkContract( ShareStrategy.getSymbol());
  		    
			//oContrat = (Contract) oTIMApiWrapper.createContract(ShareStrategy.getSymbol(), ShareStrategy.getSecurity_type(),ShareStrategy.getExchange(),oMarket.getCurrency(), _Expiration, "", 0);

			/* ESTO HACE FALTA?????? */
			OrderDAO.addOrder(oTIMApiWrapper.getLastPositionID(), ShareStrategy.getShareId().intValue());

			// colocamos operacion de venta			
			Order BuyPositionTWS = new Order();
			
			String _OperationTYPE = "";
			double ValueLimit =0.0;
			// MyActualPosition contiene la operacion abierta.
			// controlamos los limites de salida con beneficio
			
			
			// 04.02.2013...Descartados los precios limitados en los cierres --> a mercado
			
			if (MyActualPosition.getType().equals(PositionStates.statusTWSFire.BUY.toString())) // operacion de compra normal abierta
			{
				_OperationTYPE = PositionStates.statusTWSFire.SELL.toString();
				
				// se cierra con una venta cuyo limite esta por encima
				
				//ValueLimit = ValueToSell  + (ValueToSell * ShareStrategy.getPercentual_limit_buy());
			}
			else  // venta inicial
			{
				_OperationTYPE = PositionStates.statusTWSFire.BUY.toString();
				//ValueLimit = ValueToSell  - (ValueToSell * ShareStrategy.getPercentual_limit_buy());
			}						
			
			///LogTWM.log(Priority.INFO, "Stop de " + _OperationTYPE);
			
			BuyPositionTWS.account(this.getACCOUNT_NAME());
			BuyPositionTWS.action(_OperationTYPE);   				
			//BuyPositionTWS.m_totalQuantity = MyActualPosition.getShare_number().intValue();
			
			BuyPositionTWS.totalQuantity(MyActualPosition.getShare_number_to_trade().intValue());
			//BuyPositionTWS.m_orderType = PositionStates.ordertypes.LMT.toString();
			BuyPositionTWS.orderType(PositionStates.ordertypes.MKT.toString());	
			//BuyPositionTWS.m_allOrNone = true;
			
			
			/* FORMATEAMOS A DOS CIFRAS */
			BuyPositionTWS.lmtPrice( Utilidades.RedondeaPrice(ValueLimit));
			BuyPositionTWS.auxPrice(Utilidades.RedondeaPrice(ValueLimit));
			
			
			/* DIARIA 
			BuyPositionTWS.m_tif = "DAY";*/			
			/* VALIDA HASTA LA FECHA DE HOY YYYYMMDD 
			BuyPositionTWS.m_goodTillDate = Utilidades.getDateNowFormat();*/
			
			//int TicketTWSToBuy = tickerId+1;
			// abrimos orden
			int LastPositionID = Utilidades.LastPositionIDTws(oTIMApiWrapper);

			
			
			MyActualPosition.setPositionID_tws_sell(new Long(LastPositionID));
			MyActualPosition.setLimit_price_sell(ValueLimit);
			MyActualPosition.setPrice_sell(ValueToSell);
			MyActualPosition.setState_sell(PositionStates.statusTWSCallBack.PendingSubmit.toString());
			/* si metemos el date sell en las parciales, no entran las siguientes */
			/* acumulo las acciones vendidas y a vender en la operativa */
			int _RemaingShares = MyActualPosition.getShare_number_to_trade().intValue() +MyActualPosition.getShare_number_traded().intValue();
			int _TotalShares = MyActualPosition.getShare_number().intValue();
			if (_RemaingShares==_TotalShares)  // ultimo paquete de acciones
			{	
				MyActualPosition.setDate_sell(new Timestamp(Calendar.getInstance().getTimeInMillis()));
			}
			MyActualPosition.setRealtimeID_sell_alert(RealTimeID);			  
			MyActualPosition.setDescription("Stop Profit.");
			MyActualPosition.setStrategyID_sell(ConfigKeys.STRATEGY_SELL_STOP_PROFIT);
			
			
			/* ACTUALIZAMOS LA OPERACION CON LOS DATOS DE LA VENTA. ACTUALIZAMOS MAS EN EL ORDER STATUS */
			PositionDAO.updatePositionByPositionID(MyActualPosition);
			
			/* MANDAMOS CORREO		    				
			String _MailText= "[SIMULADO]." + Utilidades.getActualHourFormat() + ".Venta por Stop de Beneficio de " + ShareStrategy.getSymbol() + " a " + ValueToSell + ", " + ShareStrategy.getNumber_purchase() + " acciones ";					
			String[] Recipients = {"rfdiestro@gmail.com","trading1@ono.com","david_nevado@yahoo.es"};
			TWSMail Mail = new TWSMail(Recipients,_MailText, _MailText);
			Mail.SendMail(); */
		    /* FIN MANDAMOS CORREO */
			
			// LANZAMOS OPERACION DE VENTA.
			oTIMApiWrapper.GITraderOpenOrder(LastPositionID, oContrat, BuyPositionTWS);
			
			return true;
		
        }
			catch (Exception er)
	        {
				LogTWM.log(Priority.ERROR, er.getMessage());
				er.printStackTrace();	
				return false;
	        }
			
		}
			
	

	public boolean Strategy() {
		// TODO Auto-generated method stub
		return false;
	}

	




}
