/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tim.dao;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import com.tim.util.ConfigKeys;
import com.tim.util.LogTWM;
import com.tim.util.PositionStates;
import com.tim.util.bbdd.DoubleHandler;
import com.tim.util.bbdd.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.Priority;

import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.log4j.Log4jMLog;
import com.tim.model.Market;
import com.tim.model.Order;
import com.tim.model.Position;
import com.tim.model.Share;
import com.tim.model.SimulationPosition;

/**
 * 
 * @author je10034
 */

public class SimulationPositionDAO {

	public SimulationPositionDAO() {
	}

	

	public static boolean updatePositionByPositionID(SimulationPosition oPosicion ) {
		boolean resultadoOK = true;

		try {

			QueryRunner qr = new QueryRunner();

			String SqlPosition = "update simulation_position ";
			SqlPosition += "set shareID=##shareID##,";
			SqlPosition += "state=##state##,";
			SqlPosition += "state_buy=##state_buy##,";
			SqlPosition += "state_sell=##state_sell##,";			
			SqlPosition += "price_buy=##price_buy##,";						
			SqlPosition += "date_buy=##date_buy##,";			
			SqlPosition += "price_sell=##price_sell##,";						
			SqlPosition += "date_sell=##date_sell##,";			
			SqlPosition += "share_number=##share_number##,";
			SqlPosition += "strategyID_sell=##strategyID_sell##,";	
			SqlPosition += "simulationID=##simulationID##,";
			SqlPosition += "type=##type##";
			
			SqlPosition += " where positionId=" + oPosicion.getPositionID();

			/*
			 * LogTWM.getLog(PositionDAO.class); LogTWM.log(Priority.INFO,
			 * "Updating StatusSell:" + oPosicion.getState_sell() +
			 * ",StopProfit:" + oPosicion.getSell_price_stop_profit() +
			 * ",Operations:" + oPosicion.getTrading_data_operations() +
			 * ",PendingSharenumber:" + oPosicion.getShare_number_to_trade());
			 */
			qr.updateBean(SqlPosition, oPosicion);

		} catch (Exception e) {
			resultadoOK = false;
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "updatePositionByPositionID:" + e.getMessage());

		}
		return resultadoOK;
	}

	public static boolean addPosition(SimulationPosition oPosicion) {
		boolean resultadoOK = true;

		try {

			QueryRunner qr = new QueryRunner();
			String SqlPosition = "insert into simulation_position (";		
			SqlPosition += "shareID,";
			SqlPosition += "state,";
			SqlPosition += "state_buy,";
			SqlPosition += "state_sell,";
			SqlPosition += "limit_price_buy,";
			SqlPosition += "price_buy,";			
			SqlPosition += "date_buy,";			
			SqlPosition += "limit_price_sell,";
			SqlPosition += "price_sell,";			
			SqlPosition += "date_sell,";			
			SqlPosition += "share_number,";
			SqlPosition += "type,";
			SqlPosition += "strategyID_buy,";
			SqlPosition += "strategyID_sell,";
			SqlPosition += "sell_percentual_stop_profit,";
			SqlPosition += "sell_percentual_stop_lost,";
			SqlPosition += "share_number_to_trade,";
			SqlPosition += "simulationID,";
			SqlPosition += "share_number_traded) values (";			
			SqlPosition += "##shareID##,";
			SqlPosition += "##state##,";
			SqlPosition += "##state_buy##,";
			SqlPosition += "##state_sell##,";			
			SqlPosition += "##limit_price_buy##,";
			SqlPosition += "##price_buy##,";			
			SqlPosition += "##date_buy##,";			
			SqlPosition += "##limit_price_sell##,";
			SqlPosition += "##price_sell##,";			
			SqlPosition += "##date_sell##,";			
			SqlPosition += "##share_number##,";
			SqlPosition += "##type##,";			
			SqlPosition += "##strategyID_buy##,";
			SqlPosition += "##strategyID_sell##,";
			SqlPosition += "##sell_percentual_stop_profit##,";
			SqlPosition += "##sell_percentual_stop_lost##,";
			SqlPosition += "##share_number_to_trade##,";
			SqlPosition += "##simulationID##,";
			SqlPosition += "##share_number_traded##)";

			qr.updateBean(SqlPosition, oPosicion);

		} catch (Exception e) {
			resultadoOK = false;
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "addPosition:" + e.getMessage());

		}
		return resultadoOK;
	}

	/*
	 * una posicion abierta quiere decir que no hay venta o que esta iniciada
	 * sin cancelar
	 */

	public static boolean ExistsPositionShareOpen(int ShareId) {
		Long resultado = null;
		String Sql = "";
		try {
			java.util.List datos = new java.util.ArrayList();
			if (ShareId != -1) {
				datos.add(ShareId);
			}
			QueryRunner qr = new QueryRunner();
			ScalarHandler scl = new ScalarHandler(1);

			Sql = "select count(shareId) from simulation_position where ";
			if (ShareId != -1) {
				Sql += " shareId=? and ";
			}
			Sql += " (date_buy is null or date_sell is null)";

			resultado = (Long) qr.query(Sql, datos.toArray(), scl);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);

			LogTWM.log(Priority.FATAL, "ExistsPositionShareOpen:" + Sql + "|"
					+ e.getMessage());

		}
		return (resultado.longValue() > 0);
	}

	/*
	 * LA USAMOS COMO COMODIN PARA USAR TODAS LAS POSICIONES ABIERTAS... -1 TODO
	 */

	public static boolean ExistsPositionToSell(int share) {
		Long resultado = null;

		try {
			java.util.List datos = new java.util.ArrayList();
			datos.add(PositionStates.status.BUY_OK.toString());
			datos.add(share);
			QueryRunner qr = new QueryRunner();
			ScalarHandler scl = new ScalarHandler(1);

			String Sql = "select count(shareId) from simulation_position where ";
			Sql += "  state=? and shareId=?";
			/*
			 * 1.4.2013. cambio a raiz de que los date_sell no se rellenan en
			 * las compras parciales
			 */
			Sql += "  and state_sell is null";

			resultado = (Long) qr.query(Sql, datos.toArray(), scl);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "ExistsPositionToSell:" + e.getMessage());

		}
		return (resultado.longValue() > 0);
	}

	public static boolean ExistsPositionToClose(Long ShareId) {
		Long resultado = null;

		try {
			java.util.List datos = new java.util.ArrayList();
			datos.add(PositionStates.status.SELL_OK.toString());
			if (ShareId != null)
				datos.add(ShareId);
			QueryRunner qr = new QueryRunner();
			ScalarHandler scl = new ScalarHandler(1);

			String Sql = "select count(shareId) from simulation_position where ";
			Sql += "  state<>?";
			if (ShareId != null) {
				Sql += "  and shareId=?";
			}

			resultado = (Long) qr.query(Sql, datos.toArray(), scl);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "ExistsPositionToSell:" + e.getMessage());
			resultado = new Long(0);

		}
		return (resultado.longValue() > 0);
	}
	
	/* Type : ALL, BUY, SELL */
	public static boolean ExistsPositionToCloseByType(String Type ) {
		Long resultado = null;

		try {
			java.util.List datos = new java.util.ArrayList();
			datos.add(PositionStates.status.SELL_OK.toString());			
			QueryRunner qr = new QueryRunner();
			ScalarHandler scl = new ScalarHandler(1);

			if (!Type.equals("ALL")) {
				datos.add(Type);
			} 
			String Sql = "select count(shareId) from simulation_position where ";
			Sql += "  state<>?";
			if (!Type.equals("ALL")) {
				Sql += "  and type=?";
			}

			resultado = (Long) qr.query(Sql, datos.toArray(), scl);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "ExistsPositionToSell:" + e.getMessage());
			resultado = new Long(0);

		}
		return (resultado.longValue() > 0);
	}

	public static boolean ExistsPositionToCancel(Long ShareId) {
		Long resultado = null;

		try {
			java.util.List datos = new java.util.ArrayList();
			datos.add(ShareId);
			QueryRunner qr = new QueryRunner();
			ScalarHandler scl = new ScalarHandler(1);

			String Sql = "select count(shareId) from simulation_position where ";
			Sql += " shareId=? and pending_cancelled =1";

			resultado = (Long) qr.query(Sql, datos.toArray(), scl);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "ExistsPositionToSell:" + e.getMessage());
			resultado = new Long(0);

		}
		return (resultado.longValue() > 0);
	}

	public static List<Position> getPositionToClose(Long ShareId) {
		java.util.List<Position> resultado = null;

		try {
			java.util.List datos = new java.util.ArrayList();
			datos.add(PositionStates.status.SELL_OK.toString());
			datos.add(ShareId);
			QueryRunner qr = new QueryRunner();
			BeanListHandler blh = new BeanListHandler(Position.class);

			String Sql = "select * from simulation_position where ";
			Sql += "  state<>? and shareId=?";

			resultado = (List<Position>) qr.query(Sql, datos.toArray(), blh);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "ExistsPositionToSell:" + e.getMessage());

		}
		return (resultado);
	}

	public static Position getPositionToCancel(Long ShareId) {
		Position resultado = null;

		try {
			java.util.List datos = new java.util.ArrayList();
			datos.add(ShareId);
			QueryRunner qr = new QueryRunner();
			BeanHandler blh = new BeanHandler(Position.class);

			String Sql = "select * from simulation_position where ";
			Sql += "  and shareId=? and pending_cancelled=1";

			resultado = (Position) qr.query(Sql, datos.toArray(), blh);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "ExistsPositionToSell:" + e.getMessage());

		}
		return (resultado);
	}
	
	/* igual que getPositions pero nos aseguramos que sean del dia */
	public static java.util.List<Position> getTradingPositionsByDate(int ShareId, Timestamp oDate, int SimulationID ) {
		java.util.List<Position> resultado = null;

		try {

			java.util.List datos = new java.util.ArrayList();
			datos.add(ShareId);
			datos.add(oDate);
			datos.add(SimulationID);

			QueryRunner qr = new QueryRunner();
			BeanListHandler blh = new BeanListHandler(Position.class);

			String Sql = "select * from simulation_position where shareId=?";
			Sql += " and date(date_sell) = date(?) and simulationID=? ";
			Sql += " order by positionid desc";

			resultado = (List<Position>) qr.query(Sql, datos.toArray(), blh);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "getTradingPositionsByDate:" + e.getMessage());

		}
		return (resultado);
	}
	

	/* igual que getPositions pero nos aseguramos que sean del dia */
	public static java.util.List<Position> getTradingPositions(int ShareId) {
		java.util.List<Position> resultado = null;

		try {

			java.util.List datos = new java.util.ArrayList();
			datos.add(ShareId);

			QueryRunner qr = new QueryRunner();
			BeanListHandler blh = new BeanListHandler(Position.class);

			String Sql = "select * from simulation_position where shareId=?";
			Sql += " and date(dateadded) = date(now()) ";
			Sql += " order by positionid desc";

			resultado = (List<Position>) qr.query(Sql, datos.toArray(), blh);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "getTradingPositions:" + e.getMessage());

		}
		return (resultado);
	}

	public static java.util.List<SimulationPosition> getPositions(int ShareId,
			boolean Opened) {
		java.util.List<SimulationPosition> resultado = null;

		try {

			java.util.List datos = new java.util.ArrayList();
			datos.add(ShareId);

			QueryRunner qr = new QueryRunner();
			BeanListHandler blh = new BeanListHandler(SimulationPosition.class);

			String Sql = "select * from simulation_position where shareId=?";
			if (Opened) {
				/* VERIFICAR */
				Sql += " and date_sell is null and state<>?";
				datos.add(PositionStates.status.CANCEL_BUY);
			} else {
				Sql += " and date_sell is not null";
			}

			resultado = (List<SimulationPosition>) qr.query(Sql, datos.toArray(), blh);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "getPositions:" + e.getMessage());

		}
		return (resultado);
	}

	/*
	 * DEVOLVEMOS LA CANTIDAD DE DINERO QUE SE HA INVERTIDO EN LAS COMPRAS EN UN
	 * PERIODO, PARA UN MERCADO Y UNA ACCION SI ES INDICADO. STATE , BUY, SELL O
	 * TODOS. MARKET null TODOS SHAREID null TODOS OPCIONALES VAMOS A CONTEMPLAR
	 * TODAS LAS OPERACIONES PENDIENTES DE COMPRAR TB QUE NO HAYAN ENTRADO
	 */

	public static Double getTotalAmountPosicion(Timestamp from, Timestamp to,
			Market oMarket, Integer ShareId) {
		Double resultado = null;
		resultado = new Double(0);

		try {

			java.util.List datos = new java.util.ArrayList();

			datos.add(from);
			datos.add(to);
			datos.add(oMarket.getMarketID());

			if (ShareId != null)
				datos.add(ShareId);

			QueryRunner qr = new QueryRunner();
			DoubleHandler scl = new DoubleHandler(1);

			/* OPERACIONES DE COMPRA. VEMOS LOS ESTADOS */

			String Sql = "select floor(sum(a.price_buy* a.share_number)) as total from simulation_position a, market_share b";
			Sql += " where date(dateAdded)>=date(?)";
			Sql += " and  date(dateAdded)<=date(?)";
			Sql += " and a.shareID = b.shareId and b.marketId = ? ";
			/*
			 * METEMOS LAS PENDING_SUBMIT, SUBMITTED, PRESUBMITTED Y FILLED DE
			 * LAS COMPRAS
			 */
			Sql += " and a.state_buy in ('"
					+ PositionStates.statusTWSCallBack.Filled.toString() + "'";
			Sql += ",'"
					+ PositionStates.statusTWSCallBack.PendingSubmit.toString()
					+ "'";
			Sql += ",'"
					+ PositionStates.statusTWSCallBack.PreSubmitted.toString()
					+ "'";
			Sql += ",'" + PositionStates.statusTWSCallBack.Submitted.toString()
					+ "')";

			if (ShareId != null) {
				Sql += " and   shareID<=?";
			}
			// Y QUE NO ESTEN VENDIDAS
			Sql += " and a.date_real_sell is null";
			resultado = (Double) qr.query(Sql, datos.toArray(), scl);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL,
					"getTotalAmountPosicion:" + e.getMessage());

		}
		return (resultado);
	}

	public static Position getPosition(int PositionID) {
		Position resultado = null;

		try {
			java.util.List datos = new java.util.ArrayList();
			datos.add(PositionID);
			QueryRunner qr = new QueryRunner();
			BeanHandler blh = new BeanHandler(Position.class);
			resultado = (Position) qr.query(
					"select * from simulation_position where positionId=?",
					datos.toArray(), blh);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "getPosition:" + e.getMessage());

		}
		return resultado;
	}

	public static Position getPositionByIdSell(int PositionSellID) {
		Position resultado = null;

		try {
			java.util.List datos = new java.util.ArrayList();
			datos.add(PositionSellID);
			QueryRunner qr = new QueryRunner();
			BeanHandler blh = new BeanHandler(Position.class);
			resultado = (Position) qr.query(
					"select * from simulation_position where positionID_tws_sell=?",
					datos.toArray(), blh);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "getPositionByIdSell:" + e.getMessage());

		}
		return resultado;
	}

	// OBTIENE TODAS LAS POSICIONES ABIERTAS QUE NO ESTEN CANCELADAS NI
	// INACTIVAS
	// PARA EL DIA DE TRADING
	public static List<Position> getTradingPositions(Timestamp Fecha,
			Long Page, Long RegXPage, boolean Total, String FilterPosicion) {
		List<Position> resultado = null;

		try {
			java.util.List datos = new java.util.ArrayList();

			datos.add(Fecha);
			QueryRunner qr = new QueryRunner();
			BeanListHandler blh = new BeanListHandler(Position.class);
			String Sql = "select ";
			if (Total)
				Sql += "  positionID ";
			else {
				Sql += " positionID,	a.shareID,state,	state_buy,	IFNULL(state_sell,'') state_sell,	IFNULL(price_buy,-1) price_buy,	IFNULL(price_real_buy,-1) price_real_buy,	IFNULL(limit_price_buy,-1) limit_price_buy,	date_buy, ";
				Sql += "date_real_buy,	IFNULL(price_sell,-1) price_sell,	IFNULL(price_real_sell,-1) price_real_sell,	IFNULL(limit_price_sell,-1) limit_price_sell,	";
				Sql += "IFNULL(sell_price_stop_lost,-1) sell_price_stop_lost, IFNULL(sell_price_stop_profit,-1) sell_price_stop_profit, date_sell,	date_real_sell,	share_number, share_number_traded,share_number_to_trade,	dateAdded, ";
				Sql += " positionID_tws_sell,	a.type,	realtimeID_buy_alert,	realtimeID_sell_alert,	strategyID_buy,	strategyID_sell, IFNULL(a.sell_percentual_stop_lost,-1) sell_percentual_stop_lost, IFNULL(a.sell_percentual_stop_profit,-1) sell_percentual_stop_profit ";
				Sql += " ,b.name as shareName, symbol as shareSymbol, strategy_buy.name as strategyBuy_name,";
				Sql += " strategy_sell.name as strategySell_name,";
				Sql += " FN_GETLAST_REALTIMESHARE(a.shareID) as realtime_value, trading_data_operations ";
			}
			Sql += " from simulation_position a 	inner join 	share b ";
			Sql += " on	a.shareID = b.shareID  left join strategy strategy_buy  on a.strategyID_buy = strategy_buy.strategyID ";
			Sql += " left join strategy strategy_sell on a.strategyID_sell = strategy_sell.strategyID ";
			Sql += " where 	state_buy in ('Filled', 'PendingSubmit', 'PreSubmitted', 'Submitted')";
			Sql += " and (state_sell is null or state_sell in ('Filled', 'PendingSubmit', 'PreSubmitted', 'Submitted')) ";
			Sql += " and date(a.dateAdded) = date(?)";

			/* METEMOS LOS FILTROS */
			if (FilterPosicion
					.equalsIgnoreCase(ConfigKeys.FILTER_CONSOLA_EXECUTED)) {
				Sql += " and date_real_sell is not null ";
			}

			if (FilterPosicion.equalsIgnoreCase(ConfigKeys.FILTER_CONSOLA_OPEN)) {
				Sql += "and  date_real_sell is null ";
			}

			Sql += " order by a.dateAdded desc ";
			if (Page != null && !Total) {
				Sql += " limit  " + ((Page - 1) * RegXPage) + "," + RegXPage;

			}
			resultado = (List<Position>) qr.query(Sql, datos.toArray(), blh);

			LogTWM.getLog(SimulationPositionDAO.class);

		} catch (Exception e) {
			LogTWM.getLog(SimulationPositionDAO.class);
			LogTWM.log(Priority.FATAL, "getTradingPositions:" + e.getMessage());

		}
		return resultado;
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		Market oMarket = MarketDAO.getMarket(new Long(2));
		Double Lista = SimulationPositionDAO.getTotalAmountPosicion(new Timestamp(
				Calendar.getInstance().getTimeInMillis()), new Timestamp(
				Calendar.getInstance().getTimeInMillis()), oMarket, null);
	}

}
