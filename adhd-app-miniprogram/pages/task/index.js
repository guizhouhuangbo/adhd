const api = require('../../utils/api');

Page({
  data: {
    taskName: '',
    tasks: [],
  },

  onShow() {
    api.login()
      .then(() => this.loadTasks())
      .catch((error) => {
        wx.showToast({ title: (error && error.message) || '微信登录失败', icon: 'none' });
      });
  },

  onTaskNameInput(event) {
    this.setData({ taskName: event.detail.value });
  },

  loadTasks() {
    api.getTasks()
      .then((tasks) => {
        this.setData({ tasks });
      })
      .catch(() => {
        wx.showToast({ title: '任务加载失败', icon: 'none' });
      });
  },

  createTask() {
    const { taskName } = this.data;
    if (!taskName.trim()) {
      wx.showToast({ title: '先输入任务名', icon: 'none' });
      return;
    }
    api.createTask({ name: taskName })
      .then(() => {
        this.setData({ taskName: '' });
        this.loadTasks();
        wx.showToast({ title: '任务已创建', icon: 'success' });
      })
      .catch(() => {
        wx.showToast({ title: '创建失败', icon: 'none' });
      });
  },

  completeTask(event) {
    const { id } = event.currentTarget.dataset;
    api.checkIn(id)
      .then((res) => {
        wx.showToast({ title: `+${res.earnedStars} 星`, icon: 'success' });
        this.loadTasks();
      })
      .catch((error) => {
        wx.showToast({ title: (error && error.msg) || '打卡失败', icon: 'none' });
      });
  },
});
