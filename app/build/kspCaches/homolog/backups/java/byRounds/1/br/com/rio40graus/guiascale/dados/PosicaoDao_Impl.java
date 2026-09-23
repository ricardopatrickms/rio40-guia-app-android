package br.com.rio40graus.guiascale.dados;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PosicaoDao_Impl implements PosicaoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Posicao> __insertionAdapterOfPosicao;

  private final SharedSQLiteStatement __preparedStmtOfLimparEnviadasAntesDe;

  public PosicaoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPosicao = new EntityInsertionAdapter<Posicao>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `posicoes` (`id`,`latitude`,`longitude`,`precisao`,`velocidade`,`capturadoEm`,`mapaId`,`enviada`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Posicao entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getLatitude());
        statement.bindDouble(3, entity.getLongitude());
        if (entity.getPrecisao() == null) {
          statement.bindNull(4);
        } else {
          statement.bindDouble(4, entity.getPrecisao());
        }
        if (entity.getVelocidade() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getVelocidade());
        }
        statement.bindLong(6, entity.getCapturadoEm());
        if (entity.getMapaId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getMapaId());
        }
        final int _tmp = entity.getEnviada() ? 1 : 0;
        statement.bindLong(8, _tmp);
      }
    };
    this.__preparedStmtOfLimparEnviadasAntesDe = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM posicoes WHERE enviada = 1 AND capturadoEm < ?";
        return _query;
      }
    };
  }

  @Override
  public Object inserir(final Posicao posicao, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPosicao.insert(posicao);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object limparEnviadasAntesDe(final long antesDe,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfLimparEnviadasAntesDe.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, antesDe);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfLimparEnviadasAntesDe.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object pendentes(final int limite, final Continuation<? super List<Posicao>> $completion) {
    final String _sql = "SELECT * FROM posicoes WHERE enviada = 0 ORDER BY capturadoEm ASC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limite);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Posicao>>() {
      @Override
      @NonNull
      public List<Posicao> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfPrecisao = CursorUtil.getColumnIndexOrThrow(_cursor, "precisao");
          final int _cursorIndexOfVelocidade = CursorUtil.getColumnIndexOrThrow(_cursor, "velocidade");
          final int _cursorIndexOfCapturadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "capturadoEm");
          final int _cursorIndexOfMapaId = CursorUtil.getColumnIndexOrThrow(_cursor, "mapaId");
          final int _cursorIndexOfEnviada = CursorUtil.getColumnIndexOrThrow(_cursor, "enviada");
          final List<Posicao> _result = new ArrayList<Posicao>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Posicao _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final Float _tmpPrecisao;
            if (_cursor.isNull(_cursorIndexOfPrecisao)) {
              _tmpPrecisao = null;
            } else {
              _tmpPrecisao = _cursor.getFloat(_cursorIndexOfPrecisao);
            }
            final Float _tmpVelocidade;
            if (_cursor.isNull(_cursorIndexOfVelocidade)) {
              _tmpVelocidade = null;
            } else {
              _tmpVelocidade = _cursor.getFloat(_cursorIndexOfVelocidade);
            }
            final long _tmpCapturadoEm;
            _tmpCapturadoEm = _cursor.getLong(_cursorIndexOfCapturadoEm);
            final Integer _tmpMapaId;
            if (_cursor.isNull(_cursorIndexOfMapaId)) {
              _tmpMapaId = null;
            } else {
              _tmpMapaId = _cursor.getInt(_cursorIndexOfMapaId);
            }
            final boolean _tmpEnviada;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnviada);
            _tmpEnviada = _tmp != 0;
            _item = new Posicao(_tmpId,_tmpLatitude,_tmpLongitude,_tmpPrecisao,_tmpVelocidade,_tmpCapturadoEm,_tmpMapaId,_tmpEnviada);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object quantasPendentes(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM posicoes WHERE enviada = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object quantasDesde(final long desde, final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM posicoes WHERE capturadoEm >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, desde);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object desde(final long desde, final Continuation<? super List<Posicao>> $completion) {
    final String _sql = "SELECT * FROM posicoes WHERE capturadoEm >= ? ORDER BY capturadoEm ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, desde);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Posicao>>() {
      @Override
      @NonNull
      public List<Posicao> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfPrecisao = CursorUtil.getColumnIndexOrThrow(_cursor, "precisao");
          final int _cursorIndexOfVelocidade = CursorUtil.getColumnIndexOrThrow(_cursor, "velocidade");
          final int _cursorIndexOfCapturadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "capturadoEm");
          final int _cursorIndexOfMapaId = CursorUtil.getColumnIndexOrThrow(_cursor, "mapaId");
          final int _cursorIndexOfEnviada = CursorUtil.getColumnIndexOrThrow(_cursor, "enviada");
          final List<Posicao> _result = new ArrayList<Posicao>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Posicao _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final Float _tmpPrecisao;
            if (_cursor.isNull(_cursorIndexOfPrecisao)) {
              _tmpPrecisao = null;
            } else {
              _tmpPrecisao = _cursor.getFloat(_cursorIndexOfPrecisao);
            }
            final Float _tmpVelocidade;
            if (_cursor.isNull(_cursorIndexOfVelocidade)) {
              _tmpVelocidade = null;
            } else {
              _tmpVelocidade = _cursor.getFloat(_cursorIndexOfVelocidade);
            }
            final long _tmpCapturadoEm;
            _tmpCapturadoEm = _cursor.getLong(_cursorIndexOfCapturadoEm);
            final Integer _tmpMapaId;
            if (_cursor.isNull(_cursorIndexOfMapaId)) {
              _tmpMapaId = null;
            } else {
              _tmpMapaId = _cursor.getInt(_cursorIndexOfMapaId);
            }
            final boolean _tmpEnviada;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnviada);
            _tmpEnviada = _tmp != 0;
            _item = new Posicao(_tmpId,_tmpLatitude,_tmpLongitude,_tmpPrecisao,_tmpVelocidade,_tmpCapturadoEm,_tmpMapaId,_tmpEnviada);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object desdeDosMapas(final long desde, final List<Integer> mapaIds,
      final Continuation<? super List<Posicao>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("\n");
    _stringBuilder.append("        SELECT * FROM posicoes");
    _stringBuilder.append("\n");
    _stringBuilder.append("        WHERE capturadoEm >= ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND mapaId IN (");
    final int _inputSize = mapaIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    _stringBuilder.append("\n");
    _stringBuilder.append("        ORDER BY capturadoEm ASC");
    _stringBuilder.append("\n");
    _stringBuilder.append("        ");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 1 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, desde);
    _argIndex = 2;
    for (int _item : mapaIds) {
      _statement.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Posicao>>() {
      @Override
      @NonNull
      public List<Posicao> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfPrecisao = CursorUtil.getColumnIndexOrThrow(_cursor, "precisao");
          final int _cursorIndexOfVelocidade = CursorUtil.getColumnIndexOrThrow(_cursor, "velocidade");
          final int _cursorIndexOfCapturadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "capturadoEm");
          final int _cursorIndexOfMapaId = CursorUtil.getColumnIndexOrThrow(_cursor, "mapaId");
          final int _cursorIndexOfEnviada = CursorUtil.getColumnIndexOrThrow(_cursor, "enviada");
          final List<Posicao> _result = new ArrayList<Posicao>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Posicao _item_1;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final Float _tmpPrecisao;
            if (_cursor.isNull(_cursorIndexOfPrecisao)) {
              _tmpPrecisao = null;
            } else {
              _tmpPrecisao = _cursor.getFloat(_cursorIndexOfPrecisao);
            }
            final Float _tmpVelocidade;
            if (_cursor.isNull(_cursorIndexOfVelocidade)) {
              _tmpVelocidade = null;
            } else {
              _tmpVelocidade = _cursor.getFloat(_cursorIndexOfVelocidade);
            }
            final long _tmpCapturadoEm;
            _tmpCapturadoEm = _cursor.getLong(_cursorIndexOfCapturadoEm);
            final Integer _tmpMapaId;
            if (_cursor.isNull(_cursorIndexOfMapaId)) {
              _tmpMapaId = null;
            } else {
              _tmpMapaId = _cursor.getInt(_cursorIndexOfMapaId);
            }
            final boolean _tmpEnviada;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnviada);
            _tmpEnviada = _tmp != 0;
            _item_1 = new Posicao(_tmpId,_tmpLatitude,_tmpLongitude,_tmpPrecisao,_tmpVelocidade,_tmpCapturadoEm,_tmpMapaId,_tmpEnviada);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object marcarEnviadas(final List<Long> ids, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE posicoes SET enviada = 1 WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : ids) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
