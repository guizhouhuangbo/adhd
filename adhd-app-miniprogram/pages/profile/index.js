const api = require('../../utils/api');

Page({
  data: {
    profile: {
      parentName: 'ADHD 家长',
      childName: '小朋友',
      childAge: 8,
      totalStars: 18,
      nextReward: '积满 20 星兑换周末公园骑车',
    },
  },

  onShow() {
    api.login()
      .then(() => api.getProfile())
      .then((profile) => {
        this.setData({
          profile: {
            parentName: profile.nickname,
            childName: profile.childName,
            childAge: profile.childAge,
            totalStars: profile.totalStars,
            nextReward: '积满 20 星兑换周末公园骑车',
          },
        });
      })
      .catch((error) => {
        wx.showToast({ title: (error && error.message) || '资料加载失败', icon: 'none' });
      });
  },
});
