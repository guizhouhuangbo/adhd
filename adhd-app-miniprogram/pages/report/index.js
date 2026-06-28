const api = require('../../utils/api');

Page({
  data: {
    report: {
      highlight: '',
      suggestion: '',
      completionRate: 0,
      starsEarned: 0,
      hardestMoment: '',
    },
  },

  onShow() {
    api.login()
      .then(() => api.getWeeklyReport())
      .then((report) => {
        this.setData({ report });
      })
      .catch((error) => {
        wx.showToast({ title: (error && error.message) || '周报加载失败', icon: 'none' });
      });
  },
});
