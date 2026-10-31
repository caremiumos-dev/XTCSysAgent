package com.xtc.httplib;

import java.util.ArrayList;
import java.util.List;

/**
 * 域名配置。
 *
 * <p>{@link #HTTPS_DOMAIN_ARRAY} 是"必须走 HTTPS"的域名白名单，
 * 由 {@code PreprocessorInterceptor} 使用。这个列表是服务端与客户端之间的
 * 安全约定，内容必须与原厂逐个一致，不能删减。</p>
 */
public interface DomainConfig {

    String[] HTTPS_DOMAIN_ARRAY = {
            "watch.okii.com",
            AppFormalDomain.STORE_DOMAIN,
            "talentshow.watch.okii.com",
            AppFormalDomain.POINTS_DOMAIN,
            "oauth.watch.okii.com",
            "open.watch.okii.com",
            "chat.watch.okii.com",
            AppFormalDomain.LOCATION_DOMAIN,
            AppFormalDomain.THIRD_PARTY_DOMAIN,
            AppFormalDomain.GAME_DOMAIN,
            "weather.watch.okii.com",
            AppFormalDomain.ACTIVITY_DOMAIN,
            "energybean.watch.okii.com",
            "static.watch.okii.com",
            "sport.watch.okii.com",
            AppFormalDomain.MOMENT_DOMAIN,
            "www.watch.okii.com",
            "pay.tiancaixing.com",
            "www.pay.tiancaixing.com",
            "article.tiancaixing.com",
            "upload-file.tiancaixing.com",
            "admin.article.tiancaixing.com",
            "oper.tiancaixing.com",
            "user.tiancaixing.com",
            "gray.tiancaixing.com",
            "account.okii.com",
            "admin.eebbk.net",
            "account.eebbk.com",
            "appoffice.okii.com",
            "account.eebbk.net",
            "account.global.okii.com",
            "pay.eebbk.com",
            "paduser.xiaotiancai.com",
            "mark.eebbk.net",
            "sms.okii.com",
            AppFormalDomain.THEME_DOMAIN,
            "accountm.eebbk.net",
            "yun.imoo.com",
            "wxcrm.okii.com",
            "oauth.okii.com",
            "accountsea.eebbk.net",
            "account.imoo.com",
            "open.okii.com",
            "i18n.watch.okii.com",
            "i18n.points.okii.com",
            "i18n.weather.watch.okii.com",
            "i18n.static.watch.okii.com",
            "i18n.location.watch.okii.com",
            "i18n.open.watch.okii.com",
            "i18n.act.okii.com",
            "i18n.chat.watch.okii.com",
            "i18n.thirdparty.watch.okii.com",
            "i18n.store.watch.okii.com",
            "i18n.oauth.watch.okii.com",
            "i18n.sport.watch.okii.com",
            "i18n.game.watch.okii.com",
            "resource.watch.okii.com",
            "down.im.okii.com",
            "storefs.watch.okii.com",
            "storefs-pvt.watch.okii.com",
            "bbksmartwatch.qiniucdn.com",
            "actcdn.okii.com",
            "qiniu.video.down.im.okii.com",
            "watchcdn-pvt.okii.com",
            "watchcdn.okii.com",
            AppFormalDomain.GATEWAY_DOMAIN,
            "watch.develop.okii.com",
            "www.watch.develop.okii.com",
            "test3.okii.com",
            "static.module.watch.okii.com",
            "i18n.oauth.develop.okii.com",
            "points.develop.okii.com",
            "location.develop.okii.com",
            "act.alpha.okii.com",
            "i18n.open.alpha.okii.com",
            "i18n.static.alpha.okii.com",
            "theme.watch.module.okii.com",
            "chat.alpha.okii.com",
            "game.module.watch.okii.com",
            "store.module.okii.com",
            "i18n.game.alpha.okii.com",
            "theme.watch.develop.okii.com",
            "i18n.gw.im.module.okii.com",
            "open.alpha.okii.com",
            "act.develop.okii.com",
            "gzucloud.account.okii.com",
            "i18n.static.module.okii.com",
            "game.develop.watch.okii.com",
            "i18n.account.develop.okii.com",
            "points.module.okii.com",
            "sport.develop.watch.okii.com",
            "theme.watch.alpha.okii.com",
            "i18n.store.develop.okii.com",
            "gzucloud.account.global.okii.com",
            "i18n.thirdparty.develop.okii.com",
            "i18n.thirdparty.module.okii.com",
            "location.alpha.okii.com",
            "oauth.alpha.okii.com",
            "test.eebbk.net",
            "gzucloud.i18n.watch.okii.com",
            "i18n.game.develop.okii.com",
            "i18n.location.module.okii.com",
            "gzucloud.game.watch.okii.com",
            "ts.shop.tiancaixing.com",
            "i18n.alpha.okii.com",
            "pay.test.tiancaixing.com",
            "i18n.oauth.alpha.okii.com",
            "i18n.develop.okii.com",
            "i18n.act.alpha.okii.com",
            "gzucloud.thirdparty.watch.okii.com",
            "gzucloud.chat.watch.okii.com",
            "i18n.open.develop.okii.com",
            "chat.module.okii.com",
            "test-https.eebbk.net",
            "i18n.module.okii.com",
            "i18n.thirdparty.alpha.okii.com",
            "location.module.okii.com",
            "oauth.develop.okii.com",
            "i18n.sport.module.okii.com",
            "i18n.chat.module.okii.com",
            "thirdparty.alpha.okii.com",
            "i18n.chat.alpha.okii.com",
            "sport.alpha.watch.okii.com",
            "gzucloud.watch.okii.com",
            "i18n.sport.develop.okii.com",
            "thirdparty.module.okii.com",
            "gzucloud.sport.watch.okii.com",
            "ite.eebbk.net",
            "act.module.okii.com",
            "i18n.open.module.okii.com",
            "points.alpha.okii.com",
            "i18n.location.develop.okii.com",
            "store.develop.okii.com",
            "i18n.chat.develop.okii.com",
            "i18n.push.im.module.okii.com",
            "i18n.account.module.okii.com",
            "static.develop.watch.okii.com",
            "watch.module.okii.com",
            "i18n.location.alpha.okii.com",
            "acttest.okii.com",
            "i18n.act.module.okii.com",
            "open.develop.okii.com",
            "i18n.points.develop.okii.com",
            "i18n.points.module.okii.com",
            "i18n.gw.im.develop.okii.com",
            "i18n.store.module.okii.com",
            "open.module.okii.com",
            "static.alpha.watch.okii.com",
            "i18n.store.alpha.okii.com",
            "gzucloud.store.watch.okii.com",
            "i18n.sport.alpha.okii.com",
            "watch.alpha.okii.com",
            "gzucloud.points.okii.com",
            "chat.develop.okii.com",
            "gzucloud.static.watch.okii.com",
            "thirdparty.develop.okii.com",
            "i18n.game.module.okii.com",
            "i18n.oauth.module.okii.com",
            "i18n.static.develop.okii.com",
            "i18n.act.develop.okii.com",
            "game.alpha.watch.okii.com",
            "store.alpha.okii.com",
            "oauth.module.okii.com",
            "gzucloud.location.watch.okii.com",
            "sport.module.watch.okii.com",
            "i18n.push.im.develop.okii.com",
            "api.module.watch.okii.com"
    };

    /** 由宿主动态追加的第三方 HTTPS 域名。 */
    List<String> THIRD_HTTPS_DOMAIN_ARRAY = new ArrayList<>();

    /** 正式环境的各业务域名。 */
    interface AppFormalDomain {
        String ACTIVITY_DOMAIN = "act.okii.com";
        String CHAT_DOMAIN = "watch.okii.com";
        String DNV_NAME = "正式环境";
        String ENV_ID = "1";
        String GAME_DOMAIN = "game.watch.okii.com";
        String GATEWAY_DOMAIN = "api.watch.okii.com";
        String LOCATION_DOMAIN = "location.watch.okii.com";
        String MOMENT_DOMAIN = "moment.watch.okii.com";
        String POINTS_DOMAIN = "points.okii.com";
        String SPORT_DOMAIN = "sport.watch.okii.com";
        String STORE_DOMAIN = "store.watch.okii.com";
        String THEME_DOMAIN = "theme.watch.okii.com";
        String THIRD_PARTY_DOMAIN = "thirdparty.watch.okii.com";
        String WATCH_DOMAIN = "watch.okii.com";
        String WEATHER_DOMAIN = "sport.watch.okii.com";
    }

    /** IM 长连接域名。 */
    interface ImDomain {
        String FORMAL_DOMAIN = "gw.im.okii.com:8000";
        String[] FORMAL_STANDBY = {"106.75.86.52:1883", "106.75.86.52:8000", FORMAL_DOMAIN};
        String[] GRAY_STANDBY = new String[0];
        String GREY_DOMAIN = "gw.beta.im.okii.com:8000";
    }
}
