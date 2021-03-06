package com.jeremyliao.lebapp;

import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.jeremyliao.lebapp.databinding.ActivityLiveDataBusDemoBinding;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class LiveEventBusDemo extends AppCompatActivity {

    public static final String KEY_TEST_OBSERVE = "key_test_observe";
    public static final String KEY_TEST_OBSERVE_FOREVER = "key_test_observe_forever";
    public static final String KEY_TEST_STICKY = "key_test_sticky";
    public static final String KEY_TEST_MULTI_THREAD_POST = "key_test_multi_thread_post";
    public static final String KEY_TEST_MSG_SET_BEFORE_ON_CREATE = "key_test_msg_set_before_on_create";
    public static final String KEY_TEST_CLOSE_ALL_PAGE = "key_test_close_all_page";


    private int sendCount = 0;
    private int receiveCount = 0;
    private String randomKey = null;

    private ActivityLiveDataBusDemoBinding binding;

    private Observer<String> observer = new Observer<String>() {
        @Override
        public void onChanged(@Nullable String s) {
            Toast.makeText(LiveEventBusDemo.this, s, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_live_data_bus_demo);
        binding.setHandler(this);
        binding.setLifecycleOwner(this);
        LiveEventBus.get()
                .with(KEY_TEST_OBSERVE, String.class)
                .observe(this, new Observer<String>() {
                    @Override
                    public void onChanged(@Nullable String s) {
                        Toast.makeText(LiveEventBusDemo.this, s, Toast.LENGTH_SHORT).show();
                    }
                });
        LiveEventBus.get()
                .with(KEY_TEST_OBSERVE_FOREVER, String.class)
                .observeForever(observer);
        LiveEventBus.get()
                .with(KEY_TEST_CLOSE_ALL_PAGE, Boolean.class)
                .observe(this, new Observer<Boolean>() {
                    @Override
                    public void onChanged(@Nullable Boolean b) {
                        if (b) {
                            finish();
                        }
                    }
                });
        LiveEventBus.get()
                .with(KEY_TEST_MULTI_THREAD_POST, String.class)
                .observe(this, new Observer<String>() {
                    @Override
                    public void onChanged(@Nullable String s) {
                        receiveCount++;
                    }
                });
        testMessageSetBeforeOnCreate();
    }

    private void testMessageSetBeforeOnCreate() {
        //?????????????????????
        LiveEventBus.get().with(KEY_TEST_MSG_SET_BEFORE_ON_CREATE, String.class).setValue("msg set before");
        //????????????????????????
        LiveEventBus.get()
                .with(KEY_TEST_MSG_SET_BEFORE_ON_CREATE, String.class)
                .observe(this, new Observer<String>() {
                    @Override
                    public void onChanged(@Nullable String s) {
                        Toast.makeText(LiveEventBusDemo.this, s, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveEventBus.get()
                .with(KEY_TEST_OBSERVE_FOREVER, String.class)
                .removeObserver(observer);
    }

    public void sendMsgBySetValue() {
        Observable.just(new Random())
                .map(new Func1<Random, String>() {
                    @Override
                    public String call(Random random) {
                        return "Message By SetValue: " + random.nextInt(100);
                    }
                })
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        LiveEventBus.get().with(KEY_TEST_OBSERVE).setValue(s);
                    }
                });
    }

    public void sendMsgByPostValue() {
        Observable.just(new Random())
                .map(new Func1<Random, String>() {
                    @Override
                    public String call(Random random) {
                        return "Message By PostValue: " + random.nextInt(100);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        LiveEventBus.get().with(KEY_TEST_OBSERVE).postValue(s);
                    }
                });
    }

    public void sendMsgToForeverObserver() {
        Observable.just(new Random())
                .map(new Func1<Random, String>() {
                    @Override
                    public String call(Random random) {
                        return "Message To ForeverObserver: " + random.nextInt(100);
                    }
                })
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        LiveEventBus.get().with(KEY_TEST_OBSERVE_FOREVER).setValue(s);
                    }
                });
    }

    public void sendMsgToStickyReceiver() {
        Observable.just(new Random())
                .map(new Func1<Random, String>() {
                    @Override
                    public String call(Random random) {
                        return "Message Sticky: " + random.nextInt(100);
                    }
                })
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        LiveEventBus.get().with(KEY_TEST_STICKY).setValue(s);
                    }
                });
    }

    public void startStickyActivity() {
        startActivity(new Intent(this, StickyActivity.class));
    }

    public void startNewActivity() {
        startActivity(new Intent(this, LiveEventBusDemo.class));
    }

    public void closeAll() {
        LiveEventBus.get().with(KEY_TEST_CLOSE_ALL_PAGE).setValue(true);
    }

    public void postValueCountTest() {
        sendCount = 1000;
        receiveCount = 0;
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        for (int i = 0; i < sendCount; i++) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    LiveEventBus.get().with(KEY_TEST_MULTI_THREAD_POST).postValue("test_data");
                }
            });
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LiveEventBusDemo.this, "sendCount: " + sendCount +
                        " | receiveCount: " + receiveCount, Toast.LENGTH_LONG).show();
            }
        }, 1000);
    }

    public void testMessageSetBefore() {
        //?????????????????????key
        randomKey = "key_random_" + new Random().nextInt();
        //????????????????????????
        LiveEventBus.get().with(randomKey, String.class).setValue("msg set before");
        //????????????????????????
        LiveEventBus.get()
                .with(randomKey, String.class)
                .observe(this, new Observer<String>() {
                    @Override
                    public void onChanged(@Nullable String s) {
                        Toast.makeText(LiveEventBusDemo.this, s, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void sendMessageSetBefore() {
        LiveEventBus.get().with(randomKey, String.class).setValue("msg set after");
    }
}
