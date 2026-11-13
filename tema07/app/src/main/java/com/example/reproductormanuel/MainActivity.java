package com.example.reproductormanuel;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private SeekBar seekBar;
    private TextView tvActual, tvTotal;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable actualizarBarra = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                long posicion = player.getCurrentPosition();
                seekBar.setProgress((int) posicion);
                tvActual.setText(formatear(posicion));
                handler.postDelayed(this, 500);
            }
        }
    };

    private SoundPool soundPool;
    private final int[] sonidos = new int[4];
    private final Button[] botonesEfecto = new Button[4];
    private int cargados = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        seekBar = findViewById(R.id.seekBar);
        tvActual = findViewById(R.id.tvActual);
        tvTotal = findViewById(R.id.tvTotal);
        Button btnPlay = findViewById(R.id.btnPlay);
        Button btnPausa = findViewById(R.id.btnPausa);

        player = new ExoPlayer.Builder(this).build();
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.cancion);
        player.setMediaItem(MediaItem.fromUri(uri));

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int estado) {
                if (estado == Player.STATE_READY) {
                    long duracion = player.getDuration();
                    seekBar.setMax((int) duracion);
                    tvTotal.setText(formatear(duracion));
                }
            }
        });
        player.prepare();

        btnPlay.setOnClickListener(v -> {
            if (player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0);
            }
            player.play();
        });
        btnPausa.setOnClickListener(v -> player.pause());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progreso, boolean delUsuario) {
                if (delUsuario) {
                    tvActual.setText(formatear(progreso));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) { }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                player.seekTo(sb.getProgress());
            }
        });

        botonesEfecto[0] = findViewById(R.id.btnEfecto1);
        botonesEfecto[1] = findViewById(R.id.btnEfecto2);
        botonesEfecto[2] = findViewById(R.id.btnEfecto3);
        botonesEfecto[3] = findViewById(R.id.btnEfecto4);

        AudioAttributes atributos = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(atributos)
                .build();


        soundPool.setOnLoadCompleteListener((sp, idSonido, estado) -> {
            if (estado == 0) {
                cargados++;
                if (cargados == 4) {
                    for (Button b : botonesEfecto) {
                        b.setEnabled(true);
                    }
                }
            }
        });

        sonidos[0] = soundPool.load(this, R.raw.sonido1, 1);
        sonidos[1] = soundPool.load(this, R.raw.sonido2, 1);
        sonidos[2] = soundPool.load(this, R.raw.sonido3, 1);
        sonidos[3] = soundPool.load(this, R.raw.sonido4, 1);

        for (int i = 0; i < 4; i++) {
            final int indice = i;
            botonesEfecto[i].setOnClickListener(v ->
                    soundPool.play(sonidos[indice], 1f, 1f, 1, 0, 1f));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(actualizarBarra);
    }

    @Override
    protected void onStop() {
        super.onStop();
        player.pause();
        handler.removeCallbacks(actualizarBarra);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(actualizarBarra);
        player.release();
        soundPool.release();
    }

    private String formatear(long milisegundos) {
        int segundosTotales = (int) (milisegundos / 1000);
        return String.format(Locale.getDefault(), "%02d:%02d",
                segundosTotales / 60, segundosTotales % 60);
    }
}