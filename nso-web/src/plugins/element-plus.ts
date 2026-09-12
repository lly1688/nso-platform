import type { App } from 'vue';
import {
    ElAlert,
    ElAvatar,
    ElBadge,
    ElButton,
    ElCheckbox,
    ElCheckboxGroup,
    ElCollapse,
    ElCollapseItem,
    ElConfigProvider,
    ElDatePicker,
    ElDescriptions,
    ElDescriptionsItem,
    ElDialog,
    ElDivider,
    ElDrawer,
    ElDropdown,
    ElDropdownItem,
    ElDropdownMenu,
    ElEmpty,
    ElForm,
    ElFormItem,
    ElIcon,
    ElInput,
    ElInputNumber,
    ElLoading,
    ElOption,
    ElPagination,
    ElRadio,
    ElRadioButton,
    ElRadioGroup,
    ElSegmented,
    ElSelect,
    ElSkeleton,
    ElSkeletonItem,
    ElTabPane,
    ElTable,
    ElTableColumn,
    ElTabs,
    ElTag,
    ElTimeline,
    ElTimelineItem,
    ElTooltip,
    ElStep,
    ElSteps,
    ElUpload
} from 'element-plus';
import {
    ArrowDown,
    ArrowRight,
    Back,
    Bell,
    Box,
    Camera,
    Check,
    CircleCheck,
    CircleCheckFilled,
    Close,
    DataBoard,
    DocumentAdd,
    DocumentChecked,
    Download,
    Expand,
    Fold,
    FolderOpened,
    InfoFilled,
    List,
    Location,
    LocationFilled,
    Lock,
    Management,
    Memo,
    Menu,
    Monitor,
    OfficeBuilding,
    Operation,
    Picture,
    Plus,
    Printer,
    QuestionFilled,
    Refresh,
    Search,
    Service,
    Setting,
    Switch,
    SwitchButton,
    TakeawayBox,
    TrendCharts,
    Upload,
    User,
    UserFilled,
    Van,
    Warning,
    WarningFilled
} from '@element-plus/icons-vue';

import 'element-plus/es/components/alert/style/css';
import 'element-plus/es/components/avatar/style/css';
import 'element-plus/es/components/badge/style/css';
import 'element-plus/es/components/button/style/css';
import 'element-plus/es/components/checkbox/style/css';
import 'element-plus/es/components/checkbox-group/style/css';
import 'element-plus/es/components/collapse/style/css';
import 'element-plus/es/components/collapse-item/style/css';
import 'element-plus/es/components/date-picker/style/css';
import 'element-plus/es/components/descriptions/style/css';
import 'element-plus/es/components/descriptions-item/style/css';
import 'element-plus/es/components/dialog/style/css';
import 'element-plus/es/components/divider/style/css';
import 'element-plus/es/components/drawer/style/css';
import 'element-plus/es/components/dropdown/style/css';
import 'element-plus/es/components/dropdown-item/style/css';
import 'element-plus/es/components/dropdown-menu/style/css';
import 'element-plus/es/components/empty/style/css';
import 'element-plus/es/components/form/style/css';
import 'element-plus/es/components/form-item/style/css';
import 'element-plus/es/components/icon/style/css';
import 'element-plus/es/components/input/style/css';
import 'element-plus/es/components/input-number/style/css';
import 'element-plus/es/components/loading/style/css';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import 'element-plus/es/components/option/style/css';
import 'element-plus/es/components/pagination/style/css';
import 'element-plus/es/components/radio/style/css';
import 'element-plus/es/components/radio-button/style/css';
import 'element-plus/es/components/radio-group/style/css';
import 'element-plus/es/components/segmented/style/css';
import 'element-plus/es/components/select/style/css';
import 'element-plus/es/components/skeleton/style/css';
import 'element-plus/es/components/skeleton-item/style/css';
import 'element-plus/es/components/table/style/css';
import 'element-plus/es/components/table-column/style/css';
import 'element-plus/es/components/tabs/style/css';
import 'element-plus/es/components/tab-pane/style/css';
import 'element-plus/es/components/tag/style/css';
import 'element-plus/es/components/timeline/style/css';
import 'element-plus/es/components/timeline-item/style/css';
import 'element-plus/es/components/tooltip/style/css';
import 'element-plus/es/components/step/style/css';
import 'element-plus/es/components/steps/style/css';
import 'element-plus/es/components/upload/style/css';

// 按需注册的 Element Plus 组件。
const components = [
    ElAlert,
    ElAvatar,
    ElBadge,
    ElButton,
    ElCheckbox,
    ElCheckboxGroup,
    ElCollapse,
    ElCollapseItem,
    ElConfigProvider,
    ElDatePicker,
    ElDescriptions,
    ElDescriptionsItem,
    ElDialog,
    ElDivider,
    ElDrawer,
    ElDropdown,
    ElDropdownItem,
    ElDropdownMenu,
    ElEmpty,
    ElForm,
    ElFormItem,
    ElIcon,
    ElInput,
    ElInputNumber,
    ElOption,
    ElPagination,
    ElRadio,
    ElRadioButton,
    ElRadioGroup,
    ElSegmented,
    ElSelect,
    ElSkeleton,
    ElSkeletonItem,
    ElTabPane,
    ElTable,
    ElTableColumn,
    ElTabs,
    ElTag,
    ElTimeline,
    ElTimelineItem,
    ElTooltip,
    ElStep,
    ElSteps,
    ElUpload
];

// 全局可用的 Element Plus 图标。
const icons = {
    ArrowDown,
    ArrowRight,
    Back,
    Bell,
    Box,
    Camera,
    Check,
    CircleCheck,
    CircleCheckFilled,
    Close,
    DataBoard,
    DocumentAdd,
    DocumentChecked,
    Download,
    Expand,
    Fold,
    FolderOpened,
    InfoFilled,
    List,
    Location,
    LocationFilled,
    Lock,
    Management,
    Memo,
    Menu,
    Monitor,
    OfficeBuilding,
    Operation,
    Picture,
    Plus,
    Printer,
    QuestionFilled,
    Refresh,
    Search,
    Service,
    Setting,
    Switch,
    SwitchButton,
    TakeawayBox,
    TrendCharts,
    Upload,
    User,
    UserFilled,
    Van,
    Warning,
    WarningFilled
};

// 安装页面实际使用的组件和图标。
export function installElementPlus(app: App) {
    components.forEach((component) => app.use(component));
    app.use(ElLoading);
    Object.entries(icons).forEach(([name, component]) => app.component(name, component));
}
