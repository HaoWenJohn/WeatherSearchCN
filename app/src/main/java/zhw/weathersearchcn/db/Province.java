package zhw.weathersearchcn.db;

import org.litepal.crud.DataSupport;

/**
 * Created by zhw on 2017/7/21.
 */

public class Province extends DataSupport {
    private int id;
    private String provinceName;
    private int provinceCode;
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPrivinceName() {
        return provinceName;
    }

    public void setPrivinceName(String privinceName) {
        this.provinceName = privinceName;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }


}
