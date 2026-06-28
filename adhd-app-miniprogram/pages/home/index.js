const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    ready: false,
    loginStatus: '未登录',
    dashboard: {
      userName: '家长朋友',
      childName: '小朋友',
      totalStars: 0,
      streakDays: 0,
      encouragement: '今天也一起陪孩子走得更稳一点。',
      todayTasks: [],
    },
  },

  onShow() {
    this.syncLoginStatus();
    api.login()
      .then(() => {
        this.syncLoginStatus();
        return this.loadDashboard();
      })
      .catch((error) => {
        this.syncLoginStatus();
        wx.showToast({ title: (error && error.message) || '微信登录失败', icon: 'none' });
      });
  },

  syncLoginStatus() {
    this.setData({
      loginStatus: app.globalData.token ? '已登录，可请求后端接口' : '未登录，请先点按钮重新登录',
    });
  },

  handleLogin() {
    api.clearSession();
    this.syncLoginStatus();
    wx.showLoading({ title: '登录中' });
    api.login()
      .then(() => this.loadDashboard())
      .then(() => {
        this.syncLoginStatus();
        wx.hideLoading();
        wx.showToast({ title: '登录成功', icon: 'success' });
      })
      .catch((error) => {
        this.syncLoginStatus();
        wx.hideLoading();
        wx.showToast({ title: (error && error.message) || '登录失败', icon: 'none' });
      });
  },

  loadDashboard() {
    api.getDashboard()
      .then((dashboard) => {
        this.setData({ dashboard, ready: true });
      })
      .catch(() => {
        wx.showToast({ title: '先启动后端服务', icon: 'none' });
      });
  },

  openChat() {
    wx.switchTab({ url: '/pages/chat/index' });
  },
});
