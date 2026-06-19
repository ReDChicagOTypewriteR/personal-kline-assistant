import { createApp } from 'vue'
import {
  Button,
  Card,
  Collapse,
  DatePicker,
  Empty,
  Input,
  InputNumber,
  Layout,
  Menu,
  Modal,
  Radio,
  Select,
  Spin,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Upload
} from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './style.css'
import './styles/monochrome.css'
import './styles/light-terminal.css'
import App from './App.vue'
import router from './router'

const app = createApp(App);

[
  Button,
  Card,
  Collapse,
  DatePicker,
  Empty,
  Input,
  InputNumber,
  Layout,
  Menu,
  Modal,
  Radio,
  Select,
  Spin,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Upload
].forEach((component) => app.use(component))

app.use(router).mount('#app')
