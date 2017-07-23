package zhw.weathersearchcn;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import zhw.weathersearchcn.db.City;
import zhw.weathersearchcn.db.Country;
import zhw.weathersearchcn.db.Province;
import zhw.weathersearchcn.util.HttpUtil;
import zhw.weathersearchcn.util.Utility;

import static android.content.ContentValues.TAG;

/**
 * Created by zhw on 2017/7/21.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTRY=2;
    private ProgressDialog mProgressDialog;
    private TextView mTitleTextView;
    private Button backButton;
    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private List<String> dataList=new ArrayList<>();
    /**
     * 省列表
     */
    private  List<Province> mProvinceList;
    /**
     * 市列表
     */
    private List<City> mCityList;
    /**
     * 县列表
     */
    private List<Country> mCountryList;
    /**
     * 选中的省
     */
    private Province selectedProvince;
    /**
     * 选中的市
     */
    private City selectedCity;
    /**
     * 选中的级别
     */
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container,false);
        mTitleTextView=(TextView)view.findViewById(R.id.title_text);
        backButton=(Button)view.findViewById(R.id.back_button);
        mListView=(ListView)view.findViewById(R.id.list_view);
        mAdapter=new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        mListView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (currentLevel==LEVEL_PROVINCE){
                    selectedProvince=mProvinceList.get(position);
                    queryCities();
                }else if (currentLevel==LEVEL_CITY){
                    selectedCity=mCityList.get(position);
                    queryCountries();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel==LEVEL_COUNTRY)
                    queryCities();
                else if (currentLevel==LEVEL_CITY)
                    queryProvinces();
            }
        });
        queryProvinces();
    }

    /**
     * 查询省内所有城市，优先从数据库查询，若数据库无内容，则向服务器请求查询
     */
    private void queryCities(){
        mTitleTextView.setText(selectedProvince.getPrivinceName());
        backButton.setVisibility(View.VISIBLE);
        mCityList=DataSupport.where("provinceId = ?"
                ,String.valueOf(selectedProvince.getId())).find(City.class);
        if (mCityList.size()>0){
            dataList.clear();
            for (City city:mCityList)
                dataList.add(city.getCityName());
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }

    }

    /**
     * 查询市内所有县，优先从数据库查询，若数据库无内容，则向服务器请求查询
     */
    private void queryCountries(){
        mTitleTextView.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        mCountryList=DataSupport.where("CityId = ?",String.valueOf(selectedCity.getId())).find(Country.class);
        if (mCountryList.size()>0){
            dataList.clear();
            for (Country country:mCountryList)
                dataList.add(country.getCountryName());
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"
                    +provinceCode+"/"+cityCode;
            queryFromServer(address,"country");
        }
    }

    /**
     * 查询全国所有省，优先从数据库查询，若数据库无内容，则向服务器请求查询
     */
    private void queryProvinces(){
        mTitleTextView.setText("中国");
        backButton.setVisibility(View.GONE);
        mProvinceList= DataSupport.findAll(Province.class);
        if (mProvinceList.size()>0){
            dataList.clear();
            for (Province province:mProvinceList)
                dataList.add(province.getPrivinceName());
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            String address="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }
    private void queryFromServer(String address, final String searchType){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(searchType)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if ("city".equals(searchType)){
                    result=Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if ("country".equals(searchType)){
                    Log.i(TAG, "onResponse: 开始初始化县表");
                    result=Utility.handleCountyResponse(responseText,selectedCity.getId());
                    Log.i(TAG, "onResponse: 县表处理结果："+result);
                }
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(searchType)){
                                queryProvinces();
                            }else if("city".equals(searchType)){
                                queryCities();
                            }else if ("country".equals(searchType)){
                                queryCountries();
                            }
                        }
                    });
                }
            }
        });
    }
    /**
     * 显示对话框
     */
    private void showProgressDialog(){
        if (mProgressDialog==null){
            mProgressDialog=new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载。。。");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }
    /**
     * 关闭对话框
     */
    private void closeProgressDialog(){
        if (mProgressDialog!=null){
            mProgressDialog.dismiss();
        }
    }
}
