/*
 * Copyright (C) 2023 杭州白书科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.playedu.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.extern.log4j.Log4j2;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import xyz.playedu.common.constant.ConfigConstant;
import xyz.playedu.common.domain.AppConfig;
import xyz.playedu.common.exception.ServiceException;
import xyz.playedu.common.mapper.AppConfigMapper;
import xyz.playedu.common.service.AppConfigService;
import xyz.playedu.common.types.LdapConfig;
import xyz.playedu.common.types.config.S3Config;
import xyz.playedu.common.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Log4j2
public class AppConfigServiceImpl extends ServiceImpl<AppConfigMapper, AppConfig>
        implements AppConfigService {

    private Environment environment;

    @Override
    public Map<String, Long> allKeys() {
        return list().stream().collect(Collectors.toMap(AppConfig::getKeyName, AppConfig::getId));
    }

    @Override
    public List<AppConfig> allShow() {
        return list(query().getWrapper().eq("is_hidden", 0));
    }

    @Override
    public void saveFromMap(HashMap<String, String> data) {
        Map<String, AppConfig> configs =
                list(query().getWrapper().in("key_name", data.keySet())).stream()
                        .collect(Collectors.toMap(AppConfig::getKeyName, e -> e));
        List<AppConfig> list = new ArrayList<>();

        data.forEach(
                (keyNameValue, keyValueValue) -> {
                    if (keyValueValue == null) {
                        return;
                    }
                    if ("******".equals(keyNameValue)) { // 私密信息默认place
                        return;
                    }
                    AppConfig configItem = configs.get(keyNameValue);
                    if (configItem == null) { // 不存在的配置
                        return;
                    }
                    if (keyValueValue.equals(configItem.getKeyValue())) { // 没有变化
                        return;
                    }
                    list.add(
                            new AppConfig() {
                                {
                                    setId(configItem.getId());
                                    setKeyValue(keyValueValue);
                                }
                            });
                });

        if (!list.isEmpty()) {
            updateBatchById(list);
        }
    }

    @Override
    public Map<String, String> keyValues() {
        return list(query().getWrapper().eq("is_hidden", 0)).stream()
                .collect(Collectors.toMap(AppConfig::getKeyName, AppConfig::getKeyValue));
    }

    @Override
    public S3Config getS3Config() {
        // 全部的系统配置
        Map<String, String> config = keyValues();

        S3Config s3Config = new S3Config();
        s3Config.setAccessKey(config.get(ConfigConstant.MINIO_ACCESS_KEY));
        s3Config.setSecretKey(config.get(ConfigConstant.MINIO_SECRET_KEY));
        s3Config.setBucket(config.get(ConfigConstant.MINIO_BUCKET));
        s3Config.setEndpoint(config.get(ConfigConstant.MINIO_ENDPOINT));
        s3Config.setDomain(config.get(ConfigConstant.MINIO_DOMAIN));
        s3Config.setRegion(null);
        s3Config.setService("minio");

        if (StringUtil.isEmpty(s3Config.getAccessKey())) {
            s3Config.setAccessKey(environment.getProperty("MINIO_USERNAME"));
        }
        if (StringUtil.isEmpty(s3Config.getSecretKey())) {
            s3Config.setSecretKey(environment.getProperty("MINIO_PASSWORD"));
        }
        if (StringUtil.isEmpty(s3Config.getBucket())) {
            s3Config.setBucket(environment.getProperty("MINIO_BUCKET"));
        }
        if (StringUtil.isEmpty(s3Config.getEndpoint())) {
            s3Config.setEndpoint(environment.getProperty("MINIO_ENDPOINT"));
        }
        if (StringUtil.isEmpty(s3Config.getDomain())) {
            s3Config.setDomain(environment.getProperty("MINIO_DOMAIN"));
        }

        if (s3Config.getService().equals("minio") && StringUtil.isNotEmpty(s3Config.getDomain())) {
            String _domain = s3Config.getDomain();
            // 移除 / 后缀
            if (StringUtil.endsWith(_domain, "/")) {
                _domain = _domain.substring(0, _domain.length() - 1);
            }
            // 判断是否携带了bucket
            if (!StringUtil.endsWith(_domain, s3Config.getBucket())) {
                _domain += "/" + s3Config.getBucket();
            }
            s3Config.setDomain(_domain);
        }

        return s3Config;
    }

    @Override
    public boolean enabledLdapLogin() {
        AppConfig appConfig =
                getOne(query().getWrapper().eq("key_name", ConfigConstant.LDAP_ENABLED));
        return "1".equals(appConfig.getKeyValue());
    }

    @Override
    public String defaultAvatar() {
        AppConfig appConfig =
                getOne(query().getWrapper().eq("key_name", ConfigConstant.MEMBER_DEFAULT_AVATAR));
        return appConfig.getKeyValue();
    }

    @Override
    public LdapConfig ldapConfig() {
        Map<String, String> config = keyValues();

        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setEnabled(config.get(ConfigConstant.LDAP_ENABLED).equals("1"));
        ldapConfig.setUrl(config.get(ConfigConstant.LDAP_URL));
        ldapConfig.setAdminUser(config.get(ConfigConstant.LDAP_ADMIN_USER));
        ldapConfig.setAdminPass(config.get(ConfigConstant.LDAP_ADMIN_PASS));
        ldapConfig.setBaseDN(config.get(ConfigConstant.LDAP_BASE_DN));

        if (!ldapConfig.getEnabled()) {
            throw new ServiceException("LDAP服务未启用");
        }

        if (ldapConfig.getUrl().isEmpty()
                || ldapConfig.getAdminUser().isEmpty()
                || ldapConfig.getAdminPass().isEmpty()
                || ldapConfig.getBaseDN().isEmpty()) {
            throw new ServiceException("LDAP服务未配置");
        }

        return ldapConfig;
    }
}
