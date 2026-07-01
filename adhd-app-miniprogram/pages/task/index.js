const api = require('../../utils/api');

const DEFAULT_FOCUS_MINUTES = 10;
const MIN_FOCUS_MINUTES = 5;
const MAX_FOCUS_MINUTES = 30;
const GENERIC_ENCOURAGEMENTS = [
  '先盯住第一小步，做一点也算开始。',
  '你已经在坚持了，慢一点也没关系。',
  '先完成当前这一口，后面再说下一口。',
  '你不是要一次做完，只是先坚持这一轮。',
  '能坐在这里继续，就是在进步。',
];

const TASK_ENCOURAGEMENT_MAP = {
  homework: [
    '先把眼前这一题做完，就已经很棒了。',
    '不会的题先圈出来，先把会的题往前做。',
    '只盯住这一页的一小块，不着急全部做完。',
  ],
  tidy: [
    '先收眼前这一小堆，房间会一点点变整齐。',
    '不用一下收完，先完成手边这 3 样。',
    '每放回一件东西，都是在往前走。',
  ],
  bedtime: [
    '先完成这一小步，晚上就会更顺一点。',
    '先把睡前流程走稳，不用一口气全做好。',
    '慢慢来，把这一步做好就很好。',
  ],
  schoolbag: [
    '先检查一本书，书包就会更有条理。',
    '先把明天要用的东西装进去，已经很不错。',
    '一步一步来，书包会很快整理好。',
  ],
};

const BREAK_MINUTES = 3;
const SENSORY_PLANS = [
  {
    id: 'prepare-focus',
    title: '写作业前热身',
    subtitle: '先让身体醒一醒，再进入专注状态',
    durationLabel: '约 3 分钟',
    tag: '专注前',
    theme: 'mint',
    steps: [
      { title: '推墙 20 下', detail: '双手推墙，身体微微前倾，慢慢发力。', seconds: 40, icon: '👐' },
      { title: '原地跳 15 下', detail: '像小弹簧一样轻轻跳，唤醒身体。', seconds: 35, icon: '🦘' },
      { title: '抱书深压 30 秒', detail: '把书或抱枕抱在胸前，深呼吸。', seconds: 30, icon: '📚' },
    ],
  },
  {
    id: 'after-school',
    title: '放学后放电',
    subtitle: '先把身体的冲劲放掉一点，再进入家庭任务',
    durationLabel: '约 4 分钟',
    tag: '放学后',
    theme: 'sun',
    steps: [
      { title: '青蛙跳 10 下', detail: '双脚分开蹲下，再跳起来。', seconds: 40, icon: '🐸' },
      { title: '熊爬 30 秒', detail: '手脚着地慢慢向前爬，保持节奏。', seconds: 30, icon: '🐻' },
      { title: '搬 3 本书到桌上', detail: '感受手臂发力，把东西搬到指定位置。', seconds: 45, icon: '📦' },
    ],
  },
  {
    id: 'bedtime-calm',
    title: '睡前安静输入',
    subtitle: '把身体慢慢安静下来，帮助过渡到睡前流程',
    durationLabel: '约 3 分钟',
    tag: '睡前',
    theme: 'lavender',
    steps: [
      { title: '抱枕深压 30 秒', detail: '抱住枕头或被子，慢慢压一压。', seconds: 30, icon: '🛏️' },
      { title: '靠墙呼吸 5 次', detail: '背轻轻靠墙，吸气数 4 下，呼气数 4 下。', seconds: 40, icon: '🌬️' },
      { title: '慢慢拉伸双臂', detail: '手举高，再放下，做 5 次。', seconds: 35, icon: '🧘' },
    ],
  },
];

function formatClock(totalSeconds) {
  const safeSeconds = Math.max(0, totalSeconds);
  const minutes = Math.floor(safeSeconds / 60);
  const seconds = safeSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

Page({
  countdownTimer: null,
  encouragementTimer: null,

  data: {
    taskName: '',
    tasks: [],
    taskListAnchor: '',
    creatingTask: false,
    focusMinutes: DEFAULT_FOCUS_MINUTES,
    minFocusMinutes: MIN_FOCUS_MINUTES,
    maxFocusMinutes: MAX_FOCUS_MINUTES,
    activeTaskId: null,
    activeTaskName: '先选择一个任务开始专注',
    focusTotalSeconds: DEFAULT_FOCUS_MINUTES * 60,
    focusRemainingSeconds: DEFAULT_FOCUS_MINUTES * 60,
    focusClockText: formatClock(DEFAULT_FOCUS_MINUTES * 60),
    focusProgress: 0,
    focusStatusText: '先选任务，再开始一轮短专注',
    focusButtonText: '开始专注',
    focusRunning: false,
    focusFinished: false,
    focusOverlayVisible: false,
    focusEncouragement: '准备好后，我们先只专注这一小轮。',
    focusBackgroundClass: 'focus-overlay--warmup',
    showCelebration: false,
    restingMode: false,
    restSeconds: BREAK_MINUTES * 60,
    restClockText: formatClock(BREAK_MINUTES * 60),
    sensoryPlans: SENSORY_PLANS,
    sensoryOverlayVisible: false,
    activeSensoryPlan: null,
    activeSensoryStepIndex: 0,
    sensoryStepSeconds: 0,
    sensoryStepClockText: '00:00',
    sensoryStepProgress: 0,
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

  onUnload() {
    this.clearCountdownTimer();
    this.clearEncouragementTimer();
    this.clearSensoryTimer();
  },

  onHide() {
    if (this.data.focusRunning) {
      this.setData({
        focusRunning: false,
        focusOverlayVisible: true,
        focusStatusText: '已暂停，回到页面后可以继续这一轮',
        focusButtonText: '继续专注',
        focusEncouragement: '暂停一下也可以，准备好后再继续。',
      });
    }
    this.clearCountdownTimer();
    this.clearEncouragementTimer();
    this.clearSensoryTimer();
  },

  loadTasks() {
    api.getTasks()
      .then((tasks) => {
        this.setData({ tasks }, () => this.syncActiveTask(tasks));
      })
      .catch(() => {
        wx.showToast({ title: '任务加载失败', icon: 'none' });
      });
  },

  syncActiveTask(tasks) {
    if (!Array.isArray(tasks) || !tasks.length) {
      this.clearCountdownTimer();
      this.setData({
        activeTaskId: null,
        activeTaskName: '先创建一个任务，再开始专注',
        focusRunning: false,
        focusFinished: false,
        focusOverlayVisible: false,
        showCelebration: false,
        focusStatusText: '先添加任务，再开始一轮短专注',
        focusButtonText: '开始专注',
        focusEncouragement: '准备好后，我们先只专注这一小轮。',
        focusBackgroundClass: 'focus-overlay--warmup',
        restingMode: false,
        restSeconds: BREAK_MINUTES * 60,
        restClockText: formatClock(BREAK_MINUTES * 60),
      });
      return;
    }

    const currentTask = tasks.find((task) => task.id === this.data.activeTaskId && !task.completed);
    if (currentTask) {
      this.setData({ activeTaskName: currentTask.name });
      return;
    }

    const nextTask = tasks.find((task) => !task.completed) || tasks[0];
    if (!nextTask) {
      return;
    }

    this.resetTaskSelection(nextTask.id, nextTask.name, true);
  },

  onFocusMinutesChange(event) {
    const nextMinutes = Number(event.detail.value);
    if (!nextMinutes || this.data.focusRunning) {
      return;
    }

    const totalSeconds = nextMinutes * 60;
    this.setData({
      focusMinutes: nextMinutes,
      focusTotalSeconds: totalSeconds,
      focusRemainingSeconds: totalSeconds,
      focusClockText: formatClock(totalSeconds),
      focusProgress: 0,
      focusFinished: false,
      focusStatusText: this.data.activeTaskId ? `已设置 ${nextMinutes} 分钟，按开始进入这一轮短专注` : '先选任务，再开始一轮短专注',
      focusButtonText: '开始专注',
    });
  },

  selectTaskForFocus(event) {
    const { id, name } = event.currentTarget.dataset;
    const nextTaskId = Number(id);

    if (this.data.focusRunning && nextTaskId !== this.data.activeTaskId) {
      wx.showToast({ title: '请先完成当前专注', icon: 'none' });
      return;
    }

    this.resetTaskSelection(nextTaskId, name, false);
  },

  handleFocusAction() {
    if (!this.data.activeTaskId) {
      wx.showToast({ title: '先选一个任务', icon: 'none' });
      return;
    }

    if (this.data.restingMode) {
      this.resetFocusSession('准备好了，开始下一轮短专注');
      this.startFocus();
      return;
    }

    if (this.data.focusRunning) {
      this.pauseFocus();
      return;
    }

    if (this.data.focusFinished || this.data.focusRemainingSeconds <= 0) {
      this.resetFocusSession('再来一轮，保持短专注就很棒');
      return;
    }

    this.startFocus();
  },

  startFocusFromTask(event) {
    this.selectTaskForFocus(event);
    setTimeout(() => {
      this.handleFocusAction();
    }, 0);
  },

  startFocus() {
    this.clearCountdownTimer();
    this.clearEncouragementTimer();
    const firstEncouragement = this.pickEncouragement(this.data.activeTaskName);
    this.setData({
      focusRunning: true,
      focusFinished: false,
      focusOverlayVisible: true,
      focusStatusText: `正在专注：${this.data.activeTaskName}`,
      focusButtonText: '暂停专注',
      focusEncouragement: firstEncouragement,
      focusBackgroundClass: this.resolveFocusBackgroundClass(this.data.focusProgress),
      showCelebration: false,
      restingMode: false,
    });
    this.scheduleEncouragements();

    this.countdownTimer = setInterval(() => {
      const nextRemaining = this.data.focusRemainingSeconds - 1;
      if (nextRemaining <= 0) {
        this.finishFocus();
        return;
      }

      this.updateFocusCountdown(nextRemaining);
    }, 1000);
  },

  pauseFocus() {
    this.clearCountdownTimer();
    this.clearEncouragementTimer();
    this.setData({
      focusRunning: false,
      focusOverlayVisible: true,
      focusStatusText: '已暂停，缓一口气后再继续',
      focusButtonText: '继续专注',
      focusEncouragement: '喝口水也行，回来继续这一小轮。',
      restingMode: false,
    });
  },

  resetFocusSession(statusText) {
    this.clearCountdownTimer();
    this.clearEncouragementTimer();
    const totalSeconds = this.data.focusTotalSeconds;
    this.setData({
      focusRemainingSeconds: totalSeconds,
      focusClockText: formatClock(totalSeconds),
      focusProgress: 0,
      focusRunning: false,
      focusFinished: false,
      focusOverlayVisible: false,
      showCelebration: false,
      focusStatusText: statusText,
      focusButtonText: '开始专注',
      focusEncouragement: '准备好后，我们先只专注这一小轮。',
      focusBackgroundClass: 'focus-overlay--warmup',
      restingMode: false,
      restSeconds: BREAK_MINUTES * 60,
      restClockText: formatClock(BREAK_MINUTES * 60),
    });
  },

  resetFocus() {
    if (!this.data.activeTaskId) {
      wx.showToast({ title: '先选一个任务', icon: 'none' });
      return;
    }
    this.resetFocusSession(`已重置：${this.data.activeTaskName}`);
  },

  finishFocus() {
    this.clearCountdownTimer();
    this.clearEncouragementTimer();
    this.setData({
      focusRemainingSeconds: 0,
      focusClockText: '00:00',
      focusProgress: 100,
      focusRunning: false,
      focusOverlayVisible: true,
      restingMode: false,
      focusStatusText: '这一轮完成了，正在推进当前步骤...',
      focusEncouragement: '你已经完成这一轮专注了。',
    });

    api.completeFocusRound(this.data.activeTaskId)
      .then((task) => {
        const finishedAllSteps = task.currentStepIndex >= task.steps.length;
        this.updateTaskInList(task);
        this.setData({
          focusFinished: true,
          focusStatusText: finishedAllSteps
            ? `所有步骤都点亮了，可以直接完成 ${task.name}`
            : `第 ${task.currentStepIndex} 步已经点亮，继续下一步也很好`,
          focusButtonText: '再来一轮',
          focusEncouragement: finishedAllSteps
            ? '太棒了，所有小步骤都完成了，去拿下整项任务吧。'
            : '很好，我们已经推进了一步，慢慢往前就行。',
          focusBackgroundClass: 'focus-overlay--celebrate',
          showCelebration: true,
        });
        wx.vibrateShort({ type: 'medium' });
        wx.showToast({ title: '已推进一步', icon: 'success' });
      })
      .catch((error) => {
        this.setData({
          focusFinished: true,
          focusStatusText: `这一轮完成了，稍后手动刷新也可以`,
          focusButtonText: '再来一轮',
          focusEncouragement: '这一轮已经完成了，先别否定自己。',
          focusBackgroundClass: 'focus-overlay--celebrate',
          showCelebration: true,
        });
        wx.vibrateShort({ type: 'medium' });
        wx.showToast({ title: (error && error.msg) || '步骤推进失败', icon: 'none' });
      });
  },

  updateFocusCountdown(remainingSeconds) {
    const elapsed = this.data.focusTotalSeconds - remainingSeconds;
    const progress = Math.min(100, Math.round((elapsed / this.data.focusTotalSeconds) * 100));
    this.setData({
      focusRemainingSeconds: remainingSeconds,
      focusClockText: formatClock(remainingSeconds),
      focusProgress: progress,
      focusBackgroundClass: this.resolveFocusBackgroundClass(progress),
    });
  },

  clearCountdownTimer() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
    }
  },

  clearSensoryTimer() {
    if (this.sensoryTimer) {
      clearInterval(this.sensoryTimer);
      this.sensoryTimer = null;
    }
  },

  updateTaskInList(updatedTask) {
    if (!updatedTask || !updatedTask.id) {
      return;
    }
    const nextTasks = this.data.tasks.map((task) => (task.id === updatedTask.id ? updatedTask : task));
    this.setData({ tasks: nextTasks });
  },

  clearEncouragementTimer() {
    if (this.encouragementTimer) {
      clearInterval(this.encouragementTimer);
      this.encouragementTimer = null;
    }
  },

  scheduleEncouragements() {
    this.clearEncouragementTimer();
    this.encouragementTimer = setInterval(() => {
      if (!this.data.focusRunning) {
        this.clearEncouragementTimer();
        return;
      }
      this.setData({ focusEncouragement: this.pickEncouragement(this.data.activeTaskName) });
    }, 60000);
  },

  pickEncouragement(taskName) {
    const taskType = this.detectTaskType(taskName);
    const pool = TASK_ENCOURAGEMENT_MAP[taskType] || GENERIC_ENCOURAGEMENTS;
    const index = Math.floor(Math.random() * pool.length);
    return pool[index];
  },

  detectTaskType(taskName) {
    const normalized = String(taskName || '');
    if (/(作业|数学|语文|英语|练习|试卷|题)/.test(normalized)) {
      return 'homework';
    }
    if (/(整理|收拾|玩具|桌子|房间)/.test(normalized)) {
      return 'tidy';
    }
    if (/(睡前|洗澡|刷牙|睡觉|睡衣)/.test(normalized)) {
      return 'bedtime';
    }
    if (/(书包|文具|课程表)/.test(normalized)) {
      return 'schoolbag';
    }
    return 'generic';
  },

  resolveFocusBackgroundClass(progress) {
    if (progress >= 80) {
      return 'focus-overlay--final';
    }
    if (progress >= 45) {
      return 'focus-overlay--steady';
    }
    return 'focus-overlay--warmup';
  },

  closeFocusOverlay() {
    if (this.data.focusRunning) {
      return;
    }
    this.setData({ focusOverlayVisible: false });
  },

  openSensoryPlan(event) {
    const { id } = event.currentTarget.dataset;
    const plan = this.data.sensoryPlans.find((item) => item.id === id);
    if (!plan) {
      return;
    }

    this.clearSensoryTimer();
    const firstStep = plan.steps[0];
    this.setData({
      sensoryOverlayVisible: true,
      activeSensoryPlan: plan,
      activeSensoryStepIndex: 0,
      sensoryStepSeconds: firstStep.seconds,
      sensoryStepClockText: formatClock(firstStep.seconds),
      sensoryStepProgress: 0,
    });
  },

  closeSensoryOverlay() {
    this.clearSensoryTimer();
    this.setData({
      sensoryOverlayVisible: false,
      activeSensoryPlan: null,
      activeSensoryStepIndex: 0,
      sensoryStepSeconds: 0,
      sensoryStepClockText: '00:00',
      sensoryStepProgress: 0,
    });
  },

  startSensoryStep() {
    const plan = this.data.activeSensoryPlan;
    if (!plan) {
      return;
    }
    const step = plan.steps[this.data.activeSensoryStepIndex];
    if (!step) {
      return;
    }

    this.clearSensoryTimer();
    this.setData({
      sensoryStepSeconds: step.seconds,
      sensoryStepClockText: formatClock(step.seconds),
      sensoryStepProgress: 0,
    });

    this.sensoryTimer = setInterval(() => {
      const nextSeconds = this.data.sensoryStepSeconds - 1;
      if (nextSeconds <= 0) {
        this.finishSensoryStep();
        return;
      }

      const progress = Math.min(100, Math.round(((step.seconds - nextSeconds) / step.seconds) * 100));
      this.setData({
        sensoryStepSeconds: nextSeconds,
        sensoryStepClockText: formatClock(nextSeconds),
        sensoryStepProgress: progress,
      });
    }, 1000);
  },

  finishSensoryStep() {
    this.clearSensoryTimer();
    const plan = this.data.activeSensoryPlan;
    if (!plan) {
      return;
    }

    const nextIndex = this.data.activeSensoryStepIndex + 1;
    if (nextIndex >= plan.steps.length) {
      wx.showToast({ title: '热身完成', icon: 'success' });
      this.setData({
        sensoryStepSeconds: 0,
        sensoryStepClockText: '00:00',
        sensoryStepProgress: 100,
      });
      return;
    }

    const nextStep = plan.steps[nextIndex];
    this.setData({
      activeSensoryStepIndex: nextIndex,
      sensoryStepSeconds: nextStep.seconds,
      sensoryStepClockText: formatClock(nextStep.seconds),
      sensoryStepProgress: 0,
    });
  },

  nextSensoryStep() {
    const plan = this.data.activeSensoryPlan;
    if (!plan) {
      return;
    }
    const nextIndex = this.data.activeSensoryStepIndex + 1;
    if (nextIndex >= plan.steps.length) {
      wx.showToast({ title: '这一套已经做完', icon: 'success' });
      return;
    }
    const nextStep = plan.steps[nextIndex];
    this.clearSensoryTimer();
    this.setData({
      activeSensoryStepIndex: nextIndex,
      sensoryStepSeconds: nextStep.seconds,
      sensoryStepClockText: formatClock(nextStep.seconds),
      sensoryStepProgress: 0,
    });
  },

  applySensoryThenFocus() {
    this.closeSensoryOverlay();
    if (!this.data.activeTaskId) {
      return;
    }
    this.startFocus();
  },

  handleImmediateCheckIn() {
    if (!this.data.activeTaskId) {
      return;
    }
    api.checkIn(this.data.activeTaskId)
      .then((res) => {
        wx.showToast({ title: `+${res.earnedStars} 星`, icon: 'success' });
        this.setData({ focusOverlayVisible: false, showCelebration: false });
        this.loadTasks();
      })
      .catch((error) => {
        wx.showToast({ title: (error && error.msg) || '打卡失败', icon: 'none' });
      });
  },

  startRestMode() {
    this.clearCountdownTimer();
    this.clearEncouragementTimer();
    const restTotalSeconds = BREAK_MINUTES * 60;
    this.setData({
      restingMode: true,
      focusOverlayVisible: true,
      focusRunning: false,
      focusFinished: false,
      showCelebration: false,
      restSeconds: restTotalSeconds,
      restClockText: formatClock(restTotalSeconds),
      focusStatusText: '先休息 3 分钟，等会再回来继续也很好。',
      focusEncouragement: '休息是为了下一轮更稳，不是放弃。',
      focusBackgroundClass: 'focus-overlay--rest',
    });

    this.countdownTimer = setInterval(() => {
      const nextRestSeconds = this.data.restSeconds - 1;
      if (nextRestSeconds <= 0) {
        this.clearCountdownTimer();
        this.setData({
          restingMode: false,
          restSeconds: 0,
          restClockText: '00:00',
          focusStatusText: '休息结束了，准备好就开始下一轮。',
          focusEncouragement: '我们再只做一小轮就好。',
          focusBackgroundClass: 'focus-overlay--warmup',
        });
        return;
      }

      this.setData({
        restSeconds: nextRestSeconds,
        restClockText: formatClock(nextRestSeconds),
      });
    }, 1000);
  },

  resetTaskSelection(taskId, taskName, silent) {
    const totalSeconds = this.data.focusMinutes * 60;
    this.clearCountdownTimer();
    this.clearEncouragementTimer();
    this.setData({
      activeTaskId: taskId,
      activeTaskName: taskName,
      focusTotalSeconds: totalSeconds,
      focusRemainingSeconds: totalSeconds,
      focusClockText: formatClock(totalSeconds),
      focusProgress: 0,
      focusRunning: false,
      focusFinished: false,
      focusOverlayVisible: false,
      showCelebration: false,
      focusStatusText: `已选任务：${taskName}`,
      focusButtonText: '开始专注',
      focusEncouragement: '准备好后，我们先只专注这一小轮。',
      focusBackgroundClass: 'focus-overlay--warmup',
      restingMode: false,
      restSeconds: BREAK_MINUTES * 60,
      restClockText: formatClock(BREAK_MINUTES * 60),
    });
    if (!silent) {
      wx.showToast({ title: '已选当前任务', icon: 'none' });
    }
  },

  createTask() {
    const { taskName } = this.data;
    if (!taskName.trim()) {
      wx.showToast({ title: '先输入任务名', icon: 'none' });
      return;
    }

    this.setData({ creatingTask: true });
    api.createTask({ name: taskName })
      .then((task) => {
        const nextTaskId = task && task.id ? String(task.id) : '';
        this.setData({
          taskName: '',
          creatingTask: false,
          taskListAnchor: nextTaskId ? `task-${nextTaskId}` : '',
        });
        this.loadTasks();
        if (task && task.id) {
          this.resetTaskSelection(task.id, task.name, true);
        }
        wx.showToast({ title: '任务已创建', icon: 'success' });
      })
      .catch((error) => {
        this.setData({ creatingTask: false });
        wx.showToast({ title: (error && error.msg) || '创建失败', icon: 'none' });
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
