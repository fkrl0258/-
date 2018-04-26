package madvirus.gallery;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.SQLException;

import madvirus.sequence.Sequencer;
import util.JdbcUtil;

public class ThemeManager {
	
	private static ThemeManager instance = new ThemeManager();
	
	public static ThemeManager getInstance() {
		return instance;
	}
	
	private ThemeManager() {
	}
	private Connection getConnection() throws Exception{
		return DriverManager.getConnection("jdbc:apache:commons:dbcp:/poo");
	}
	public void insert(Theme theme) throws Exception{
		
		Connection conn = null;
		Statement stmtGroup = null;
		ResultSet rsGroup = null;
		
		PreparedStatement pstmtOrder = null;
		ResultSet rsOrder = null;
		PreparedStatement pstmtOrderUpdate = null;
		
		PreparedStatement pstmtInsertMessage = null;
		PreparedStatement pstmtInsertContent = null;
		
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			
			if(theme.getParentId() == 0) {
				stmtGroup = conn.createStatement();
				rsGroup = stmtGroup.executeQuery("select max(GROUP_ID) from THEME_MESSAGE_00");
				int maxGroupId = 0;
				if(rsGroup.next()) {
					maxGroupId = rsGroup.getInt(1);
				}
				maxGroupId++;
				
				theme.setGroupId(maxGroupId);
				theme.setOrderNo(0);
			}else {
				pstmtOrder = conn.prepareStatement("select max(ORDER_NO) from THEME_MASSAGE_00 "
						+ " where PAREMT_ID = ? or THEME_MESSAGE_ID = ?");
				pstmtOrder.setInt(1, theme.getParentId());
				pstmtOrder.setInt(2, theme.getParentId());
				rsOrder = pstmtOrder.executeQuery();
				int maxOrder = 0;
				if(rsOrder.next()) {
					maxOrder = rsOrder.getInt(1);
				}
				maxOrder ++;
				theme.setOrderNo(maxOrder);
			}
			if(theme.getOrderNo() > 0) {
				pstmtOrderUpdate = conn.prepareStatement("update THEME_MASSAGE_00 set ORDER_NO +1 "
						+ " where GROUP_ID =? and ORDER_NO >= ?");
				pstmtOrderUpdate.setInt(1, theme.getGroupId());
				pstmtOrderUpdate.setInt(2, theme.getOrderNo());
				pstmtOrderUpdate.executeUpdate();
			}
			
			theme.setId(Sequencer.nextId(conn, "THEME_MASSAGE_00"));
			
			pstmtInsertMessage = conn.prepareStatement("insert int THEME_MASSAGE_00 values (?,?,?,?,?,?,?,?,?,?,?)");
			pstmtInsertMessage.setInt(1, theme.getId());
			pstmtInsertMessage.setInt(2, theme.getGroupId());
			pstmtInsertMessage.setInt(3, theme.getOrderNo());
			pstmtInsertMessage.setInt(4, theme.getLevels());
			pstmtInsertMessage.setInt(5, theme.getParentId());
			pstmtInsertMessage.setTimestamp(6, theme.getRegister());
			pstmtInsertMessage.setString(7, theme.getName());
			pstmtInsertMessage.setString(8, theme.getEmail());
			pstmtInsertMessage.setString(9, theme.getImage());
			pstmtInsertMessage.setString(10, theme.getPassword());
			pstmtInsertMessage.setString(11, theme.getTitle());
			pstmtInsertMessage.executeUpdate();
			
			pstmtInsertContent = conn.prepareStatement("insert into THEME_CONTENT_00 value (?,?)");
			pstmtInsertContent.setInt(1, theme.getId());
			pstmtInsertContent.setCharacterStream(2, new StringReader(theme.getContent()), theme.getContent().length());;
			pstmtInsertContent.executeUpdate();
			
			conn.commit();
		}catch(SQLException ex) {
			ex.printStackTrace();
			try {
				conn.rollback();
			}catch(SQLException ex1) {}
			
			throw new Exception("insert", ex);
		}finally {
			JdbcUtil.close(rsGroup);
			JdbcUtil.close(stmtGroup);
			JdbcUtil.close(rsOrder);
			JdbcUtil.close(pstmtOrder);
			JdbcUtil.close(pstmtOrderUpdate);
			JdbcUtil.close(pstmtInsertMessage);
			JdbcUtil.close(pstmtInsertContent);
			if(conn != null)
				try {
					conn.setAutoCommit(true);
					conn.close();
				}catch(SQLException ex) {}
		}
	}
	public void update(Theme theme) throws Exception{
		
		Connection conn = null;
		PreparedStatement pstmtUpdateMessage = null;
		PreparedStatement pstmtUpdateContent = null;
		
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			
			pstmtUpdateMessage = conn.prepareStatement("update THEME_MESSAGE_00 set NAME=?, EMAIL=?, IMAGE=?, TITLE=? "
					+ " where THEME_MESSAGE_ID=?");
			pstmtUpdateContent = conn.prepareStatement("update THEME_CONTENT_00 set CONTENT = ? "
					+ " where THEME_MESSAGE_ID=?");
			
			pstmtUpdateMessage.setString(1, theme.getName());
			pstmtUpdateMessage.setString(2, theme.getEmail());
			pstmtUpdateMessage.setString(3, theme.getImage());
			pstmtUpdateMessage.setString(4, theme.getTitle());
			pstmtUpdateMessage.setInt(5, theme.getId());
			pstmtUpdateMessage.executeUpdate();
			
			pstmtUpdateContent.setCharacterStream(1, new StringReader(theme.getContent()), theme.getContent().length());
			pstmtUpdateContent.setInt(2, theme.getId());
			pstmtUpdateContent.executeUpdate();
			
			conn.commit();
		}catch(SQLException ex) {
			ex.printStackTrace();
			try {
				conn.rollback();
			}catch(SQLException ex1) {}
			throw new Exception("update", ex);
		}finally {
			JdbcUtil.close(pstmtUpdateMessage);
			JdbcUtil.close(pstmtUpdateContent);
			if(conn != null)
				try {
					conn.setAutoCommit(true);
					conn.close();
				}catch(SQLException ex) {}
		}
	}
	public int count(List whereCond, Map valueMap) throws Exception{
		
		if(valueMap == null) valueMap = Collections.EMPTY_MAP;
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			conn = getConnection();
			StringBuffer query = new StringBuffer(200);
			query.append("select count(*) from THEME_MESSAGE_00 ");
			if(whereCond != null && whereCond.size() > 0) {
				query.append("where ");
				for(int i=0; i<whereCond.size(); i++) {
					query.append(whereCond.get(i));
					if(i<whereCond.size() -1) {
						query.append(" or ");
					}
				}
			}
			pstmt = conn.prepareStatement(query.toString());
			
			Iterator keyIter = valueMap.keySet().iterator();
			while(keyIter.hasNext()) {
				Integer key = (Integer) keyIter.next();
				Object obj = valueMap.get(key);
				if(obj instanceof String) {
					pstmt.setString(key.intValue(), (String)obj);
				}else if (obj instanceof Integer) {
					pstmt.setInt(key.intValue(), ((Integer)obj).intValue());
				}else if (obj instanceof Timestamp) {
					pstmt.setTimestamp(key.intValue(), (Timestamp)obj);
				}
			}
			rs = pstmt.executeQuery();
			int count = 0;
			if(rs.next()) {
				count = rs.getInt(1);
			}
			return count;
		}catch(SQLException ex) {
			ex.printStackTrace();
			throw new Exception("count", ex);
		}finally {
			JdbcUtil.close(rs);
			JdbcUtil.close(pstmt);
			JdbcUtil.close(conn);
		}
	}
	public List selectList(List whereCond, Map valueMap, int startRow, int endRow) throws Exception{
		
		if(valueMap == null) valueMap = Collections.EMPTY_MAP;
		
		Connection conn = null;
		PreparedStatement pstmtMessage = null;
		ResultSet rsMessage = null;
		
		try {
			StringBuffer query = new StringBuffer(200);
			
			query.append("select * from ( ");
			query.append("		select THEME_MESSAGE_ID,GROUP_ID, ORDER_NO, LEVELS, PARENT_ID, REGISTER, NAME, EMAIL, IMAGE, PASSWORD, TITLE, ROWNUM rnum ");
			query.append("		from ( ");
			query.append("			select THEME_MESSAGE_ID,GROUP_ID, ORDER_NO, LEVELS, PARENT_ID, REGISTER, NAME, EMAIL, IMAGE, PASSWORD, TITLE");
			query.append("from THEME_MESSAGE_00 ");
			if(whereCond != null && whereCond.size() > 0) {
				query.append("where ");
				for(int i=0; i<whereCond.size(); i++) {
					query.append(whereCond.get(i));
					if(i<whereCond.size() -1 ) {
						query.append(" or ");
						
					}
				}
			}
			query.append("		order by GROUP_ID desc, ORDER_NO asc ");
			query.append("	) where ROWNUM <=? ");
			query.append(") where rnum >= ? ");
			
			conn = getConnection();
			
			pstmtMessage = conn.prepareStatement(query.toString());
			Iterator keyIter = valueMap.keySet().iterator();
			while(keyIter.hasNext()) {
				Integer key = (Integer)keyIter.next();
				Object obj = valueMap.get(key);
				if (obj instanceof String) {
					pstmtMessage.setString(key.intValue(), (String)obj);
				}else if (obj instanceof Integer) {
					pstmtMessage.setInt(key.intValue(), ((Integer)obj).intValue());
				}else if (obj instanceof Timestamp) {
					pstmtMessage.setTimestamp(key.intValue(), (Timestamp)obj);
				}
			}
			pstmtMessage.setInt(valueMap.size()+1, endRow +1);
			pstmtMessage.setInt(valueMap.size()+2, startRow +1);
			
			rsMessage = pstmtMessage.executeQuery();
			if(rsMessage.next()) {
				List list = new java.util.ArrayList(endRow-startRow+1);
				
				do {
					Theme theme = new Theme();
					theme.setId(rsMessage.getInt("THEME_MESSAGE_ID"));
					theme.setGroupId(rsMessage.getInt("GROUP_ID"));
					theme.setOrderNo(rsMessage.getInt("ORDER_NO"));
					theme.setLevels(rsMessage.getInt("LEVELS"));
					theme.setParentId(rsMessage.getInt("PARENT_ID"));
					theme.setRegister(rsMessage.getTimestamp("REGISTER"));
					theme.setName(rsMessage.getString("NAME"));
					theme.setEmail(rsMessage.getString("EMAIL"));
					theme.setImage(rsMessage.getString("IMAGE"));
					theme.setPassword(rsMessage.getString("PASSWORD"));
					theme.setTitle(rsMessage.getString("TITLE"));
					list.add(theme);
				}while(rsMessage.next());
				
				return list;
			}else {
				return Collections.EMPTY_LIST;
			}
		}catch(SQLException ex) {
			ex.printStackTrace();
			throw new Exception("selectList", ex);
		}finally {
			JdbcUtil.close(rsMessage);
			JdbcUtil.close(pstmtMessage);
			JdbcUtil.close(conn);
		}
	}
	
	public Theme select(int id) throws Exception{
		
		Connection conn = null;
		PreparedStatement pstmtMessage = null;
		ResultSet rsMessage = null;
		PreparedStatement pstmtContent = null;
		ResultSet rsContent = null;
		
		try {
			Theme theme = null;
			
			conn = getConnection();
			pstmtMessage = conn.prepareStatement("select * from THEME_MESSAGE_00 "
					+ "where THEME_MESSAGE_ID = ?");
			pstmtMessage.setInt(1, id);
			rsMessage = pstmtMessage.executeQuery();
			if(rsMessage.next()) {
				theme = new Theme();
				theme.setId(rsMessage.getInt("THEME_MESSAGE_ID"));
				theme.setGroupId(rsMessage.getInt("GROUP_ID"));
				theme.setOrderNo(rsMessage.getInt("ORDER_NO"));
				theme.setLevels(rsMessage.getInt("LEVELS"));
				theme.setParentId(rsMessage.getInt("PARENT_ID"));
				theme.setRegister(rsMessage.getTimestamp("REGISTER"));
				theme.setName(rsMessage.getString("NAME"));
				theme.setEmail(rsMessage.getString("EMAIL"));
				theme.setImage(rsMessage.getString("IMAGE"));
				theme.setPassword(rsMessage.getString("PASSWORD"));
				theme.setTitle(rsMessage.getString("TITLE"));
				
				pstmtContent = conn.prepareStatement("select CONTENT from THEME_CONTENT_00 "
						+ " where THEME_MASSAGE_ID =?");
				pstmtContent.setInt(1, id);
				rsContent = pstmtContent.executeQuery();
				if(rsContent.next()) {
					Reader reader = null;
					try {
						reader = rsContent.getCharacterStream("CONTENT");
						char[] buff = new char[512];
						int len = -1;
						StringBuffer buffer = new StringBuffer(512);
						while((len = reader.read(buff)) != -1) {
							buffer.append(buff, 0, len);
						}
						theme.setContent(buffer.toString());
					}catch(IOException iex) {
						throw new Exception("select", iex);
					}finally {
						if(reader != null)
							try {
								reader.close();
							}catch(IOException iex) {}
					}
				}else {
					return null;
				}
				return theme;
			}else {
				return null;
			}
		}catch(SQLException ex) {
			ex.printStackTrace();
			throw new Exception("select", ex);
		}finally {
			JdbcUtil.close(rsMessage);
			JdbcUtil.close(pstmtMessage);
			JdbcUtil.close(rsContent);
			JdbcUtil.close(pstmtContent);
			JdbcUtil.close(conn);
		}
	}
	public void delete(int id) throws Exception{
		
		Connection conn = null;
		PreparedStatement pstmtMessage = null;
		PreparedStatement pstmtContent = null;
		
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			
			pstmtMessage = conn.prepareStatement("delete from THEME_MESSAGE_00 where THEME_MESSAGE_ID =?");
			pstmtContent = conn.prepareStatement("delete from THEME_CONTENT_00 where THEME_MESSAGE_ID =?");
			
			pstmtMessage.setInt(1, id);
			pstmtContent.setInt(1, id);
			
			int updatedCount1 = pstmtMessage.executeUpdate();
			int updatedCount2 = pstmtContent.executeUpdate();
			
			if(updatedCount1 + updatedCount2 ==2) {
				conn.commit();
			}else {
				conn.rollback();
				throw new Exception("invalid id:" +id);
			}
		}catch(SQLException ex) {
			ex.printStackTrace();
			try {
				conn.rollback();
			}catch(SQLException ex1) {}
			throw new Exception("delete", ex);
		}finally {
			JdbcUtil.close(pstmtMessage);
			JdbcUtil.close(pstmtContent);
			if (conn != null)
				try {
					conn.setAutoCommit(true);
					conn.close();
				}catch(SQLException ex) {}
		}
	}
}
