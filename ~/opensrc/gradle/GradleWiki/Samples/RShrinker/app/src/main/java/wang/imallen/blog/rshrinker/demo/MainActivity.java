package wang.imallen.blog.rshrinker.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    /**
     * 对应的ASM code如下:
     * fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "str", "I", null, new Integer(8323856));
     * fv.visitEnd();
     */
    private static final int str=0x7f0310;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
