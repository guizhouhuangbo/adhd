App({
  globalData: {
    baseUrl: 'http://127.0.0.1:8080/api',
    user: null,
    token: '',
  },

  loginPromise: null,

  onLaunch() {
    const profile = wx.getStorageSync('userProfile');
    const token = wx.getStorageSync('token');
    if (profile) {
      this.globalData.user = profile;
    }
    if (token) {
      this.globalData.token = token;
    }
  },

  ensureLogin() {
    if (this.globalData.token) {
      return Promise.resolve(this.globalData.user);
    }

    if (this.loginPromise) {
      return this.loginPromise;
    }

    this.loginPromise = new Promise((resolve, reject) => {
      wx.login({
        success: (loginRes) => {
          if (!loginRes.code) {
            this.loginPromise = null;
            reject(new Error(loginRes.errMsg || 'wx.login 未返回 code'));
            return;
          }

          wx.request({
            url: `${this.globalData.baseUrl}/auth/login`,
            method: 'POST',
            data: {
              code: loginRes.code,
              nickname: '微信家长用户',
            },
            success: (res) => {
              const payload = res.data && res.data.data;
              if (res.statusCode >= 200 && res.statusCode < 300 && payload && payload.token) {
                this.globalData.token = payload.token;
                this.globalData.user = payload.profile;
                wx.setStorageSync('token', payload.token);
                wx.setStorageSync('userProfile', payload.profile);
                this.loginPromise = null;
                resolve(payload.profile);
                return;
              }
              this.loginPromise = null;
              reject(new Error((res.data && res.data.msg) || `登录失败(${res.statusCode})`));
            },
            fail: (error) => {
              this.loginPromise = null;
              reject(new Error(error.errMsg || '请求登录接口失败'));
            },
          });
        },
        fail: (error) => {
          this.loginPromise = null;
          reject(new Error(error.errMsg || 'wx.login 调用失败'));
        },
      });
    });

    return this.loginPromise;
  },
});
